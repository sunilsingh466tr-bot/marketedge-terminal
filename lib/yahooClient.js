/**
 * lib/yahooClient.js
 * Unified Yahoo Finance transport:
 *   - When WORKER_URL (env YAHOO_WORKER_URL / WORKER_URL or config/screener-config.json WORKER_URL)
 *     is set, routes ALL Yahoo Finance build calls through the Cloudflare Worker (/yahoo/*)
 *     so CI/GitHub Actions IPs are not rate-limited or blocked.
 *   - Otherwise falls back cleanly to direct yahoo-finance2.
 *   - Also implements the 20s-cached /quotes batch handler for local server.js testing.
 */

const { formatISTDateTime, getISTComponents } = require('./nseCalendar');

let yfSingleton = null;
async function getYahooFinanceInstance() {
  if (!yfSingleton) {
    const pkg = await import('yahoo-finance2');
    const YahooFinance = pkg.default;
    yfSingleton = new YahooFinance({ suppressNotices: ['yahooSurvey'] });
  }
  return yfSingleton;
}

function normalizeWorkerUrl(raw) {
  const s = String(raw || '').trim();
  if (!s) return '';
  if (!/^https?:\/\//i.test(s)) return '';
  return s.replace(/\/+$/, '');
}

function resolveWorkerUrl(cfg = {}) {
  return normalizeWorkerUrl(
    process.env.YAHOO_WORKER_URL ||
    process.env.WORKER_URL ||
    cfg.WORKER_URL ||
    ''
  );
}

async function fetchJsonWithTimeout(url, options = {}, timeoutMs = 20000) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const resp = await fetch(url, { ...options, signal: ctrl.signal });
    if (!resp.ok) {
      const text = await resp.text().catch(() => '');
      throw new Error(`HTTP ${resp.status} from ${url}: ${text.slice(0, 120)}`);
    }
    return await resp.json();
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Creates a Yahoo client matching yahoo-finance2 method signatures:
 *   - quote(symbols)
 *   - chart(symbol, { period1, period2, interval })
 *   - quoteSummary(symbol, { modules })
 *   - fundamentalsTimeSeries(symbol, { period1, type, module })
 */
function createYahooClient(workerUrl = '') {
  const baseUrl = normalizeWorkerUrl(workerUrl);

  if (!baseUrl) {
    return {
      mode: 'direct',
      workerUrl: '',
      async quote(symbols) {
        const yf = await getYahooFinanceInstance();
        return yf.quote(symbols);
      },
      async chart(symbol, opts) {
        const yf = await getYahooFinanceInstance();
        return yf.chart(symbol, opts);
      },
      async quoteSummary(symbol, opts) {
        const yf = await getYahooFinanceInstance();
        return yf.quoteSummary(symbol, opts);
      },
      async fundamentalsTimeSeries(symbol, opts) {
        const yf = await getYahooFinanceInstance();
        return yf.fundamentalsTimeSeries(symbol, opts);
      }
    };
  }

  return {
    mode: 'worker',
    workerUrl: baseUrl,

    async quote(symbols) {
      const symList = Array.isArray(symbols) ? symbols : [symbols];
      if (symList.length === 0) return [];
      const data = await fetchJsonWithTimeout(`${baseUrl}/yahoo/quotes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ symbols: symList })
      }, 25000);
      return Array.isArray(data?.quotes) ? data.quotes : [];
    },

    async chart(symbol, opts = {}) {
      const p1 = opts.period1 instanceof Date
        ? Math.floor(opts.period1.getTime() / 1000)
        : Math.floor(Number(opts.period1 || (Date.now() / 1000 - 500 * 86400)));
      const p2 = opts.period2 instanceof Date
        ? Math.floor(opts.period2.getTime() / 1000)
        : Math.floor(Number(opts.period2 || Date.now() / 1000));
      const interval = opts.interval || '1d';
      const u = `${baseUrl}/yahoo/chart?symbol=${encodeURIComponent(symbol)}&period1=${p1}&period2=${p2}&interval=${encodeURIComponent(interval)}`;
      const data = await fetchJsonWithTimeout(u, {}, 20000);
      return {
        meta: data?.meta || {},
        quotes: Array.isArray(data?.quotes) ? data.quotes : []
      };
    },

    async quoteSummary(symbol, opts = {}) {
      const modules = Array.isArray(opts.modules) ? opts.modules.join(',') : (opts.modules || 'assetProfile');
      const u = `${baseUrl}/yahoo/quoteSummary?symbol=${encodeURIComponent(symbol)}&modules=${encodeURIComponent(modules)}`;
      return fetchJsonWithTimeout(u, {}, 15000);
    },

    async fundamentalsTimeSeries(symbol, opts = {}) {
      const p1Date = opts.period1 ? new Date(opts.period1) : new Date('2025-05-01T00:00:00Z');
      const p1 = Math.floor(p1Date.getTime() / 1000);
      const p2 = Math.floor(Date.now() / 1000);
      const u = `${baseUrl}/yahoo/fundamentals?symbol=${encodeURIComponent(symbol)}&period1=${p1}&period2=${p2}`;
      const data = await fetchJsonWithTimeout(u, {}, 15000);
      return Array.isArray(data?.series) ? data.series : [];
    }
  };
}

// Local server 20s cache mirroring the Cloudflare Worker /quotes contract
const localLiveQuoteCache = new Map();

function formatISTShortTime(dateInput = new Date()) {
  const c = getISTComponents(new Date(dateInput));
  return `${String(c.hours).padStart(2, '0')}:${String(c.minutes).padStart(2, '0')} IST`;
}

async function fetchLiveQuotesCached(symbols, ttlSec = 20, workerUrl = '') {
  const cleanSymbols = Array.from(
    new Set(
      (symbols || [])
        .map(s => String(s || '').trim().toUpperCase())
        .filter(Boolean)
    )
  );
  if (cleanSymbols.length === 0) {
    throw new Error('symbols list is empty');
  }

  const clampedTtl = Math.min(30, Math.max(15, Math.round(Number(ttlSec) || 20)));
  const cacheKey = [...cleanSymbols].sort().join(',');
  const now = Date.now();
  const hit = localLiveQuoteCache.get(cacheKey);
  if (hit && (now - hit.fetchedAt) < clampedTtl * 1000) {
    return { ...hit.payload, cacheHit: true };
  }

  // If external Cloudflare Worker URL is configured, forward directly to it
  const extWorker = normalizeWorkerUrl(workerUrl);
  if (extWorker) {
    const extPayload = await fetchJsonWithTimeout(`${extWorker}/quotes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ symbols: cleanSymbols })
    }, 20000);
    localLiveQuoteCache.set(cacheKey, { payload: extPayload, fetchedAt: now });
    return extPayload;
  }

  // Local yahoo-finance2 batched fetch (150 symbols per chunk)
  const yf = await getYahooFinanceInstance();
  const chunkSize = 150;
  const rawList = [];
  for (let i = 0; i < cleanSymbols.length; i += chunkSize) {
    const chunk = cleanSymbols.slice(i, i + chunkSize);
    const res = await yf.quote(chunk);
    if (Array.isArray(res)) rawList.push(...res);
  }

  const quotesMap = {};
  let latestMs = 0;
  for (const q of rawList) {
    if (!q || !q.symbol || q.regularMarketPrice == null) continue;
    const tMs = q.regularMarketTime ? new Date(q.regularMarketTime).getTime() : 0;
    if (tMs > latestMs) latestMs = tMs;

    quotesMap[q.symbol] = {
      symbol: q.symbol,
      price: Number(Number(q.regularMarketPrice).toFixed(2)),
      chg: q.regularMarketChange != null ? Number(Number(q.regularMarketChange).toFixed(2)) : null,
      chgPct: q.regularMarketChangePercent != null ? Number(Number(q.regularMarketChangePercent).toFixed(2)) : null,
      prevClose: q.regularMarketPreviousClose != null ? Number(Number(q.regularMarketPreviousClose).toFixed(2)) : null,
      high52: q.fiftyTwoWeekHigh != null ? Number(Number(q.fiftyTwoWeekHigh).toFixed(2)) : null,
      low52: q.fiftyTwoWeekLow != null ? Number(Number(q.fiftyTwoWeekLow).toFixed(2)) : null,
      time: tMs ? Math.floor(tMs / 1000) : null,
      marketState: q.marketState || null
    };
  }

  const asOfDate = latestMs > 0 ? new Date(latestMs) : new Date();
  const payload = {
    asOf: asOfDate.toISOString(),
    asOfIST: formatISTShortTime(asOfDate),
    asOfDisplay: formatISTDateTime(asOfDate),
    fetchedAt: new Date().toISOString(),
    cacheTtlSeconds: clampedTtl,
    cacheHit: false,
    count: Object.keys(quotesMap).length,
    quotes: quotesMap
  };

  localLiveQuoteCache.set(cacheKey, { payload, fetchedAt: now });
  if (localLiveQuoteCache.size > 60) {
    const oldest = localLiveQuoteCache.keys().next().value;
    localLiveQuoteCache.delete(oldest);
  }

  return payload;
}

module.exports = {
  normalizeWorkerUrl,
  resolveWorkerUrl,
  createYahooClient,
  fetchLiveQuotesCached
};
