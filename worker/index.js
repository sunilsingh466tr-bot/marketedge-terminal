/**
 * worker/index.js
 * Cloudflare Worker for MarketEdge Terminal:
 *   1. Intraday Live (delayed) batched quotes (`GET /quotes?symbols=...` / `POST /quotes`)
 *      with 20-second Cloudflare Edge + memory caching (15-30s) and CORS.
 *   2. EOD Build Yahoo Finance Proxy (`/yahoo/quotes`, `/yahoo/chart`, `/yahoo/quoteSummary`, `/yahoo/fundamentals`)
 *      so GitHub Actions / Node builds route Yahoo Finance calls through Cloudflare IPs.
 *
 * Zero API keys or secrets required.
 */

const DEFAULT_CACHE_TTL_SEC = 20;
const MIN_CACHE_TTL_SEC = 15;
const MAX_CACHE_TTL_SEC = 30;
const CRUMB_TTL_MS = 25 * 60 * 1000;
const YAHOO_UA = 'Mozilla/5.0 (compatible; yahoo-finance2/4.0.6)';

let authState = {
  cookie: '',
  crumb: '',
  fetchedAt: 0,
  inflight: null
};

// Isolate-level fallback cache (secondary to Cloudflare caches.default)
const memQuoteCache = new Map();

function corsHeaders(extra = {}) {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    ...extra
  };
}

function jsonResponse(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: corsHeaders({
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders
    })
  });
}

function formatISTTime(dateInput = new Date()) {
  const d = new Date(dateInput);
  if (Number.isNaN(d.getTime())) return '--:-- IST';
  const ist = new Date(d.getTime() + 5.5 * 3600 * 1000);
  const hh = String(ist.getUTCHours()).padStart(2, '0');
  const mm = String(ist.getUTCMinutes()).padStart(2, '0');
  return `${hh}:${mm} IST`;
}

function formatISTDateTime(dateInput = new Date()) {
  const d = new Date(dateInput);
  if (Number.isNaN(d.getTime())) return '--';
  const ist = new Date(d.getTime() + 5.5 * 3600 * 1000);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const day = String(ist.getUTCDate()).padStart(2, '0');
  const mon = months[ist.getUTCMonth()];
  const year = ist.getUTCFullYear();
  const hh = String(ist.getUTCHours()).padStart(2, '0');
  const mm = String(ist.getUTCMinutes()).padStart(2, '0');
  return `${day} ${mon} ${year}, ${hh}:${mm} IST`;
}

function extractSetCookies(headers) {
  if (typeof headers.getSetCookie === 'function') {
    return headers.getSetCookie();
  }
  const single = headers.get('set-cookie');
  if (!single) return [];
  // Fallback cookie split ignoring Expires=Day, DD-Mon-YYYY commas
  return single.split(/,(?=\s*[^;,\s]+=[^;,\s]+)/);
}

async function refreshYahooAuth(force = false) {
  const now = Date.now();
  if (!force && authState.crumb && authState.cookie && (now - authState.fetchedAt) < CRUMB_TTL_MS) {
    return authState;
  }
  if (authState.inflight) {
    return authState.inflight;
  }

  authState.inflight = (async () => {
    try {
      let cookieStr = '';

      // Primary cookie seed: fc.yahoo.com
      const r1 = await fetch('https://fc.yahoo.com', {
        headers: { 'User-Agent': YAHOO_UA },
        redirect: 'manual'
      });
      const c1 = extractSetCookies(r1.headers).map(c => c.split(';')[0].trim()).filter(Boolean);
      if (c1.length > 0) {
        cookieStr = c1.join('; ');
      }

      // Secondary cookie seed if fc.yahoo.com returned none
      if (!cookieStr) {
        const r1b = await fetch('https://finance.yahoo.com/quote/AAPL', {
          headers: { 'User-Agent': YAHOO_UA },
          redirect: 'follow'
        });
        const c1b = extractSetCookies(r1b.headers).map(c => c.split(';')[0].trim()).filter(Boolean);
        if (c1b.length > 0) {
          cookieStr = c1b.join('; ');
        }
      }

      const r2 = await fetch('https://query1.finance.yahoo.com/v1/test/getcrumb', {
        headers: {
          'User-Agent': YAHOO_UA,
          ...(cookieStr ? { Cookie: cookieStr } : {}),
          origin: 'https://finance.yahoo.com',
          referer: 'https://finance.yahoo.com/quote/AAPL',
          accept: '*/*',
          'content-type': 'text/plain'
        }
      });

      if (!r2.ok) {
        throw new Error(`Yahoo getcrumb HTTP ${r2.status}`);
      }

      const crumb = (await r2.text()).trim();
      if (!crumb || crumb.includes('<')) {
        throw new Error('Yahoo returned invalid crumb');
      }

      authState.cookie = cookieStr;
      authState.crumb = crumb;
      authState.fetchedAt = Date.now();
      return authState;
    } finally {
      authState.inflight = null;
    }
  })();

  return authState.inflight;
}

async function yahooFetchJson(urlBuilder, requireCrumb = true) {
  let auth = requireCrumb ? await refreshYahooAuth(false) : authState;

  for (let attempt = 0; attempt < 2; attempt++) {
    const url = urlBuilder(auth?.crumb || '');
    const resp = await fetch(url, {
      headers: {
        'User-Agent': YAHOO_UA,
        ...(auth?.cookie ? { Cookie: auth.cookie } : {}),
        accept: 'application/json'
      }
    });

    if ((resp.status === 401 || resp.status === 403) && attempt === 0) {
      auth = await refreshYahooAuth(true);
      continue;
    }

    if (!resp.ok) {
      const txt = await resp.text().catch(() => '');
      throw new Error(`Yahoo HTTP ${resp.status}: ${txt.slice(0, 140)}`);
    }

    return await resp.json();
  }

  throw new Error('Yahoo fetch failed after crumb refresh');
}

function parseSymbolsInput(url, bodyObj) {
  let raw = [];
  const q = url.searchParams.get('symbols');
  if (q) {
    raw = q.split(',');
  } else if (bodyObj && Array.isArray(bodyObj.symbols)) {
    raw = bodyObj.symbols;
  }
  const seen = new Set();
  const clean = [];
  for (const s of raw) {
    const sym = String(s || '').trim().toUpperCase();
    if (sym && !seen.has(sym)) {
      seen.add(sym);
      clean.push(sym);
    }
  }
  return clean;
}

async function fetchYahooQuotesRaw(symbols, fields = null) {
  const chunkSize = 150;
  const chunks = [];
  for (let i = 0; i < symbols.length; i += chunkSize) {
    chunks.push(symbols.slice(i, i + chunkSize));
  }

  const results = [];
  const concurrency = 4;
  let idx = 0;

  async function worker() {
    while (idx < chunks.length) {
      const chunk = chunks[idx++];
      const symsParam = encodeURIComponent(chunk.join(','));
      const fieldsParam = fields ? `&fields=${encodeURIComponent(fields)}` : '';
      const data = await yahooFetchJson(
        (crumb) =>
          `https://query2.finance.yahoo.com/v7/finance/quote?symbols=${symsParam}${fieldsParam}&crumb=${encodeURIComponent(crumb)}`,
        true
      );
      const list = data?.quoteResponse?.result;
      if (Array.isArray(list)) {
        results.push(...list);
      }
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, chunks.length) }, () => worker()));
  return results;
}

function getCacheTtl(env) {
  const raw = Number(env?.QUOTE_CACHE_TTL || DEFAULT_CACHE_TTL_SEC);
  if (!Number.isFinite(raw)) return DEFAULT_CACHE_TTL_SEC;
  return Math.min(MAX_CACHE_TTL_SEC, Math.max(MIN_CACHE_TTL_SEC, Math.round(raw)));
}

async function handleLiveQuotes(request, url, env, ctx) {
  let bodyObj = null;
  if (request.method === 'POST') {
    try {
      bodyObj = await request.json();
    } catch (e) {
      return jsonResponse({ error: 'Invalid JSON body' }, 400);
    }
  }

  const symbols = parseSymbolsInput(url, bodyObj);
  if (symbols.length === 0) {
    return jsonResponse({ error: 'symbols parameter is required' }, 400);
  }

  const ttlSec = getCacheTtl(env);
  const sortedKey = [...symbols].sort().join(',');
  const cacheUrl = new URL(url.origin);
  cacheUrl.pathname = '/_cache/quotes';
  cacheUrl.searchParams.set('s', sortedKey);
  const cacheRequest = new Request(cacheUrl.toString(), { method: 'GET' });

  // 1. Check Cloudflare Edge Cache
  const edgeCache = typeof caches !== 'undefined' && caches.default ? caches.default : null;
  if (edgeCache) {
    const cachedResp = await edgeCache.match(cacheRequest);
    if (cachedResp) {
      const payload = await cachedResp.json();
      return jsonResponse(
        { ...payload, cacheHit: true },
        200,
        {
          'Cache-Control': `public, max-age=${ttlSec}, s-maxage=${ttlSec}`,
          'X-Worker-Cache': 'HIT-EDGE'
        }
      );
    }
  }

  // 2. Check isolate memory cache
  const now = Date.now();
  const memHit = memQuoteCache.get(sortedKey);
  if (memHit && (now - memHit.fetchedAt) < ttlSec * 1000) {
    return jsonResponse(
      { ...memHit.payload, cacheHit: true },
      200,
      {
        'Cache-Control': `public, max-age=${ttlSec}, s-maxage=${ttlSec}`,
        'X-Worker-Cache': 'HIT-MEM'
      }
    );
  }

  // 3. Fetch live quotes from Yahoo in batches of 150
  const fields = 'symbol,regularMarketPrice,regularMarketChange,regularMarketChangePercent,regularMarketPreviousClose,regularMarketTime,marketState,fiftyTwoWeekHigh,fiftyTwoWeekLow';
  const rawList = await fetchYahooQuotesRaw(symbols, fields);

  const quotesMap = {};
  let latestEpochSec = 0;

  for (const q of rawList) {
    if (!q || !q.symbol || q.regularMarketPrice == null) continue;
    const tSec = Number(q.regularMarketTime || 0);
    if (tSec > latestEpochSec) latestEpochSec = tSec;

    quotesMap[q.symbol] = {
      symbol: q.symbol,
      price: Number(Number(q.regularMarketPrice).toFixed(2)),
      chg: q.regularMarketChange != null ? Number(Number(q.regularMarketChange).toFixed(2)) : null,
      chgPct: q.regularMarketChangePercent != null ? Number(Number(q.regularMarketChangePercent).toFixed(2)) : null,
      prevClose: q.regularMarketPreviousClose != null ? Number(Number(q.regularMarketPreviousClose).toFixed(2)) : null,
      high52: q.fiftyTwoWeekHigh != null ? Number(Number(q.fiftyTwoWeekHigh).toFixed(2)) : null,
      low52: q.fiftyTwoWeekLow != null ? Number(Number(q.fiftyTwoWeekLow).toFixed(2)) : null,
      time: tSec || null,
      marketState: q.marketState || null
    };
  }

  const asOfDate = latestEpochSec > 0 ? new Date(latestEpochSec * 1000) : new Date();
  const payload = {
    asOf: asOfDate.toISOString(),
    asOfIST: formatISTTime(asOfDate),
    asOfDisplay: formatISTDateTime(asOfDate),
    fetchedAt: new Date().toISOString(),
    cacheTtlSeconds: ttlSec,
    cacheHit: false,
    count: Object.keys(quotesMap).length,
    quotes: quotesMap
  };

  memQuoteCache.set(sortedKey, { payload, fetchedAt: now });
  if (memQuoteCache.size > 60) {
    const oldestKey = memQuoteCache.keys().next().value;
    memQuoteCache.delete(oldestKey);
  }

  const response = jsonResponse(payload, 200, {
    'Cache-Control': `public, max-age=${ttlSec}, s-maxage=${ttlSec}`,
    'X-Worker-Cache': 'MISS'
  });

  if (edgeCache && ctx && typeof ctx.waitUntil === 'function') {
    ctx.waitUntil(edgeCache.put(cacheRequest, response.clone()));
  }

  return response;
}

async function handleYahooBuildQuotes(request, url) {
  let bodyObj = null;
  if (request.method === 'POST') {
    bodyObj = await request.json();
  }
  const symbols = parseSymbolsInput(url, bodyObj);
  if (symbols.length === 0) {
    return jsonResponse({ error: 'symbols parameter is required' }, 400);
  }
  const quotes = await fetchYahooQuotesRaw(symbols, null);
  return jsonResponse({ count: quotes.length, quotes }, 200, { 'Cache-Control': 'no-store' });
}

async function handleYahooChart(url) {
  const symbol = (url.searchParams.get('symbol') || '').trim();
  if (!symbol) return jsonResponse({ error: 'symbol parameter is required' }, 400);

  const period2 = Number(url.searchParams.get('period2') || Math.floor(Date.now() / 1000));
  const period1 = Number(url.searchParams.get('period1') || (period2 - 500 * 86400));
  const interval = url.searchParams.get('interval') || '1d';

  const data = await yahooFetchJson(
    () =>
      `https://query2.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(symbol)}?period1=${Math.floor(period1)}&period2=${Math.floor(period2)}&interval=${encodeURIComponent(interval)}&events=div%2Csplits`,
    false
  );

  const result = data?.chart?.result?.[0];
  if (!result) {
    const errMsg = data?.chart?.error?.description || `No chart result for ${symbol}`;
    return jsonResponse({ error: errMsg }, 404);
  }

  const timestamps = result.timestamp || [];
  const q = result.indicators?.quote?.[0] || {};
  const adj = result.indicators?.adjclose?.[0]?.adjclose || [];

  const quotes = [];
  for (let i = 0; i < timestamps.length; i++) {
    const ts = timestamps[i];
    const close = q.close?.[i];
    if (ts == null || close == null) continue;
    quotes.push({
      date: new Date(ts * 1000).toISOString(),
      open: q.open?.[i] ?? close,
      high: q.high?.[i] ?? close,
      low: q.low?.[i] ?? close,
      close,
      volume: q.volume?.[i] ?? 0,
      adjclose: adj[i] ?? close
    });
  }

  return jsonResponse(
    {
      meta: result.meta || {},
      quotes
    },
    200,
    { 'Cache-Control': 'no-store' }
  );
}

async function handleYahooQuoteSummary(url) {
  const symbol = (url.searchParams.get('symbol') || '').trim();
  const modules = (url.searchParams.get('modules') || 'assetProfile').trim();
  if (!symbol) return jsonResponse({ error: 'symbol parameter is required' }, 400);

  const data = await yahooFetchJson(
    (crumb) =>
      `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${encodeURIComponent(symbol)}?modules=${encodeURIComponent(modules)}&crumb=${encodeURIComponent(crumb)}`,
    true
  );

  const result = data?.quoteSummary?.result?.[0];
  if (!result) {
    return jsonResponse({ error: `No quoteSummary for ${symbol}` }, 404);
  }
  return jsonResponse(result, 200, { 'Cache-Control': 'no-store' });
}

async function handleYahooFundamentals(url) {
  const symbol = (url.searchParams.get('symbol') || '').trim();
  if (!symbol) return jsonResponse({ error: 'symbol parameter is required' }, 400);

  const period2 = Number(url.searchParams.get('period2') || Math.floor(Date.now() / 1000));
  const period1 = Number(url.searchParams.get('period1') || Math.floor(new Date('2025-05-01T00:00:00Z').getTime() / 1000));
  const types = 'quarterlyTotalRevenue,quarterlyNetIncome';

  const data = await yahooFetchJson(
    () =>
      `https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/${encodeURIComponent(symbol)}?type=${encodeURIComponent(types)}&period1=${Math.floor(period1)}&period2=${Math.floor(period2)}`,
    false
  );

  const rows = data?.timeseries?.result || [];
  const byDate = new Map();

  for (const item of rows) {
    const revArr = item.quarterlyTotalRevenue || [];
    for (const pt of revArr) {
      if (!pt || !pt.asOfDate) continue;
      const cur = byDate.get(pt.asOfDate) || { date: pt.asOfDate };
      cur.totalRevenue = pt.reportedValue?.raw ?? null;
      byDate.set(pt.asOfDate, cur);
    }
    const niArr = item.quarterlyNetIncome || [];
    for (const pt of niArr) {
      if (!pt || !pt.asOfDate) continue;
      const cur = byDate.get(pt.asOfDate) || { date: pt.asOfDate };
      cur.netIncome = pt.reportedValue?.raw ?? null;
      byDate.set(pt.asOfDate, cur);
    }
  }

  const series = Array.from(byDate.values()).sort((a, b) => a.date.localeCompare(b.date));
  return jsonResponse({ series }, 200, { 'Cache-Control': 'no-store' });
}

export default {
  async fetch(request, env, ctx) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, '') || '/';

    try {
      if (path === '/' || path === '/health') {
        return jsonResponse({
          ok: true,
          service: 'marketedge-yahoo-worker',
          cacheTtlSeconds: getCacheTtl(env),
          nowIST: formatISTDateTime(new Date())
        });
      }

      if (path === '/quotes') {
        return await handleLiveQuotes(request, url, env, ctx);
      }

      if (path === '/yahoo/quotes') {
        return await handleYahooBuildQuotes(request, url);
      }

      if (path === '/yahoo/chart') {
        return await handleYahooChart(url);
      }

      if (path === '/yahoo/quoteSummary') {
        return await handleYahooQuoteSummary(url);
      }

      if (path === '/yahoo/fundamentals') {
        return await handleYahooFundamentals(url);
      }

      return jsonResponse({ error: `Unknown route: ${path}` }, 404);
    } catch (err) {
      return jsonResponse({ error: err.message || 'Worker upstream error' }, 502);
    }
  }
};
