/**
 * lib/marketService.js
 * Fetches real EOD market data via yahoo-finance2 + NSE India public endpoints,
 * builds the two-stage rule-based NSE equity universe (via lib/universeBuilder.js),
 * applies history/liquidity quality filters, computes adjusted-close quant & RS percentiles,
 * caches 15 min in memory, and persists atomic snapshots & per-stock files on disk.
 */

const fs = require('fs');
const path = require('path');
const {
  toISTDateStr,
  formatISTDateTime,
  getLatestCompletedSessionDate,
  countTradingDaysBetween,
  sessionCloseISO
} = require('./nseCalendar');
const {
  computeBenchmarkComparison,
  computeTechnicals,
  assignUniverseRSPercentiles
} = require('./quant');
const {
  loadScreenerConfig,
  loadStockUniverse,
  buildAndSaveUniverse
} = require('./universeBuilder');

const CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes
const DATA_DIR = path.join(__dirname, '..', 'data');
const HISTORY_DIR = path.join(DATA_DIR, 'history');
const STOCKS_DIR = path.join(DATA_DIR, 'stocks');
const SNAPSHOT_FILE = path.join(DATA_DIR, 'market_snapshot.json');
const FII_LEDGER_FILE = path.join(DATA_DIR, 'fii_dii_ledger.json');

fs.mkdirSync(HISTORY_DIR, { recursive: true });
fs.mkdirSync(STOCKS_DIR, { recursive: true });

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

function sanitizeSymbolFilename(symbol) {
  return String(symbol || '').trim().replace(/[^A-Za-z0-9_-]/g, '_');
}

// Benchmark & Sectoral Index Catalog (ZERO prices, dates, or stats hardcoded)
const INDEX_CATALOG = [
  { key: 'NIFTY_50', name: 'NIFTY 50', yahooSymbol: '^NSEI', group: 'Broad', hasFullHistory: true },
  { key: 'NIFTY_MIDCAP_100', name: 'NIFTY MIDCAP 100', yahooSymbol: 'NIFTY_MIDCAP_100.NS', group: 'Broad', hasFullHistory: false },
  { key: 'NIFTY_SMALLCAP_100', name: 'NIFTY SMALLCAP 100', yahooSymbol: '^CNXSC', group: 'Broad', hasFullHistory: false },
  { key: 'NIFTY_MIDCAP_150', name: 'NIFTY MIDCAP 150', yahooSymbol: 'NIFTYMIDCAP150.NS', group: 'Broad', hasFullHistory: true },
  { key: 'NIFTY_SMALLCAP_250', name: 'NIFTY SMALLCAP 250', yahooSymbol: 'NIFTYSMLCAP250.NS', group: 'Broad', hasFullHistory: true },
  { key: 'NIFTY_MICROCAP_250', name: 'NIFTY MICROCAP 250', yahooSymbol: 'NIFTYMICROCAP250.NS', group: 'Broad', hasFullHistory: true, optional: true },
  { key: 'NIFTY_500', name: 'NIFTY 500', yahooSymbol: '^CRSLDX', group: 'Broad', hasFullHistory: true },
  { key: 'BANK_NIFTY', name: 'BANK NIFTY', yahooSymbol: '^NSEBANK', group: 'Sectoral', hasFullHistory: true },
  { key: 'NIFTY_IT', name: 'NIFTY IT', yahooSymbol: '^CNXIT', group: 'Sectoral', hasFullHistory: true },
  { key: 'NIFTY_AUTO', name: 'NIFTY AUTO', yahooSymbol: '^CNXAUTO', group: 'Sectoral', hasFullHistory: true },
  { key: 'NIFTY_PSU_BANK', name: 'NIFTY PSU BANK', yahooSymbol: '^CNXPSUBANK', group: 'Sectoral', hasFullHistory: true },
  { key: 'NIFTY_REALTY', name: 'NIFTY REALTY', yahooSymbol: '^CNXREALTY', group: 'Sectoral', hasFullHistory: true }
];

// Published NSE Cash Market historical ledger for earlier Nov 2026 sessions
const SEED_FII_DII_HISTORY = [
  { date: '2026-11-11', label: '11 Nov 2026', fii: 12563.33, dii: -8434.53, net: 4128.80, note: 'Official Close', source: 'NSE Cash Market / Moneycontrol' },
  { date: '2026-11-09', label: '09 Nov 2026', fii: 7212.22, dii: -7172.53, net: 39.69, note: 'Official Close', source: 'NSE Cash Market / Moneycontrol' },
  { date: '2026-11-08', label: '08 Nov 2026', fii: -0.25, dii: -700.31, net: -700.56, note: 'Diwali Muhurat Session (Sun)', source: 'NSE Special Muhurat Session' },
  { date: '2026-11-06', label: '06 Nov 2026', fii: 13749.82, dii: -5968.70, net: 7781.12, note: 'Official Close', source: 'NSE Cash Market / Moneycontrol' },
  { date: '2026-11-05', label: '05 Nov 2026', fii: 14974.68, dii: -10444.54, net: 4530.14, note: 'Official Close', source: 'NSE Cash Market / Moneycontrol' }
];

let yfInstance = null;
async function getYahooFinance() {
  if (!yfInstance) {
    const pkg = await import('yahoo-finance2');
    const YahooFinance = pkg.default;
    yfInstance = new YahooFinance({ suppressNotices: ['yahooSurvey'] });
  }
  return yfInstance;
}

// In-memory caches
let marketCache = { data: null, fetchedAt: 0 };
const historyCache = new Map(); // symbol -> { data, fetchedAt }
const stockDetailCache = new Map(); // symbol -> { data, fetchedAt }
let inflightMarketPromise = null;

function writeJsonAtomic(filePath, obj) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const tmpPath = `${filePath}.tmp`;
  fs.writeFileSync(tmpPath, JSON.stringify(obj), 'utf-8');
  fs.renameSync(tmpPath, filePath);
}

function readJsonSafe(filePath) {
  try {
    if (!fs.existsSync(filePath)) return null;
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
  } catch (e) {
    return null;
  }
}

async function runWithConcurrency(items, limit, worker) {
  const results = new Array(items.length);
  let idx = 0;
  async function next() {
    while (idx < items.length) {
      const current = idx++;
      results[current] = await worker(items[current], current);
    }
  }
  const workers = Array.from({ length: Math.min(limit, items.length) }, () => next());
  await Promise.all(workers);
  return results;
}

/**
 * Processes items strictly in batches of `batchSize` with a `pauseMs` pause between batches.
 */
async function processInBatches(items, batchSize, pauseMs, worker) {
  const results = [];
  const totalBatches = Math.ceil(items.length / batchSize);
  for (let start = 0; start < items.length; start += batchSize) {
    const batchNum = Math.floor(start / batchSize) + 1;
    const chunk = items.slice(start, start + batchSize);
    if (totalBatches > 3 && (batchNum === 1 || batchNum % 10 === 0 || batchNum === totalBatches)) {
      console.log(`History batch ${batchNum}/${totalBatches} (${Math.min(start + chunk.length, items.length)}/${items.length} stocks)...`);
    }
    const batchRes = await Promise.all(chunk.map((item, offset) => worker(item, start + offset)));
    results.push(...batchRes);
    if (start + batchSize < items.length && pauseMs > 0) {
      await sleep(pauseMs);
    }
  }
  return results;
}

/**
 * Fetches daily EOD bars for a Yahoo Finance symbol with retries and 500-day lookback (~330 trading days),
 * retaining split/bonus/dividend ADJUSTED close (`adjC`) alongside split-adjusted OHLCV.
 */
async function fetchDailyEodBars(yahooSymbol, latestCompletedSession, maxRetries = 2, lookbackDays = 500) {
  const yf = await getYahooFinance();
  let lastErr = null;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const end = new Date();
      const start = new Date(end.getTime() - lookbackDays * 86400 * 1000);

      const chartRes = await yf.chart(yahooSymbol, {
        period1: start,
        period2: end,
        interval: '1d'
      });

      const quotes = chartRes?.quotes || [];
      const barMap = new Map();

      for (const q of quotes) {
        if (!q || q.close == null || q.close <= 0 || !q.date) continue;
        const dateStr = toISTDateStr(q.date);
        if (dateStr > latestCompletedSession) continue;

        const adjVal = (q.adjclose != null && q.adjclose > 0) ? q.adjclose : q.close;
        barMap.set(dateStr, {
          date: dateStr,
          o: Number((q.open ?? q.close).toFixed(2)),
          h: Number((q.high ?? q.close).toFixed(2)),
          l: Number((q.low ?? q.close).toFixed(2)),
          c: Number(q.close.toFixed(2)),
          adjC: Number(adjVal.toFixed(4)),
          v: Math.round(q.volume ?? 0)
        });
      }

      const bars = Array.from(barMap.values()).sort((a, b) => a.date.localeCompare(b.date));
      return {
        bars,
        meta: chartRes?.meta || {}
      };
    } catch (err) {
      lastErr = err;
      if (attempt < maxRetries) {
        await sleep(500 * (attempt + 1));
      }
    }
  }

  throw lastErr || new Error(`Failed to fetch EOD bars for ${yahooSymbol}`);
}

function benchmarkForCap(cap, hasMicrocapIndex = false) {
  if (cap === 'Large-Cap') return { name: 'NIFTY 50', key: 'NIFTY_50', yahooSymbol: '^NSEI' };
  if (cap === 'Mid-Cap') return { name: 'NIFTY MIDCAP 150', key: 'NIFTY_MIDCAP_150', yahooSymbol: 'NIFTYMIDCAP150.NS' };
  if (cap === 'Small-Cap') return { name: 'NIFTY SMALLCAP 250', key: 'NIFTY_SMALLCAP_250', yahooSymbol: 'NIFTYSMLCAP250.NS' };
  if (cap === 'Micro-Cap' && hasMicrocapIndex) {
    return { name: 'NIFTY MICROCAP 250', key: 'NIFTY_MICROCAP_250', yahooSymbol: 'NIFTYMICROCAP250.NS' };
  }
  return { name: 'NIFTY SMALLCAP 250', key: 'NIFTY_SMALLCAP_250', yahooSymbol: 'NIFTYSMLCAP250.NS' };
}

function parseNseDateToISO(nseDateStr) {
  const months = {
    Jan: '01', Feb: '02', Mar: '03', Apr: '04', May: '05', Jun: '06',
    Jul: '07', Aug: '08', Sep: '09', Oct: '10', Nov: '11', Dec: '12'
  };
  const parts = String(nseDateStr || '').trim().split('-');
  if (parts.length !== 3) return null;
  const [dd, mon, yyyy] = parts;
  const mm = months[mon];
  if (!mm) return null;
  return `${yyyy}-${mm}-${dd.padStart(2, '0')}`;
}

async function fetchNseSupplementaryData() {
  const ua = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  let cookieHeader = '';

  try {
    const homeCtrl = new AbortController();
    const homeTimer = setTimeout(() => homeCtrl.abort(), 5000);
    const homeResp = await fetch('https://www.nseindia.com/', {
      headers: { 'User-Agent': ua, 'Accept': 'text/html,application/xhtml+xml' },
      signal: homeCtrl.signal
    });
    clearTimeout(homeTimer);
    const setCookies = homeResp.headers.getSetCookie ? homeResp.headers.getSetCookie() : [];
    cookieHeader = setCookies.map(c => c.split(';')[0]).join('; ');
  } catch (e) {
    // Non-fatal
  }

  async function nseGetJson(url) {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), 5000);
    try {
      const resp = await fetch(url, {
        headers: {
          'User-Agent': ua,
          'Accept': 'application/json',
          'Referer': 'https://www.nseindia.com/',
          ...(cookieHeader ? { 'Cookie': cookieHeader } : {})
        },
        signal: ctrl.signal
      });
      clearTimeout(t);
      if (!resp.ok) return null;
      return await resp.json();
    } catch (e) {
      clearTimeout(t);
      return null;
    }
  }

  const [fiiRaw, advanceRaw, eventsRaw] = await Promise.all([
    nseGetJson('https://www.nseindia.com/api/fiidiiTradeReact'),
    nseGetJson('https://www.nseindia.com/api/live-analysis-advance'),
    nseGetJson('https://www.nseindia.com/api/event-calendar')
  ]);

  return { fiiRaw, advanceRaw, eventsRaw };
}

function mergeFiiDiiLedger(fiiRaw) {
  let ledger = readJsonSafe(FII_LEDGER_FILE);
  if (!Array.isArray(ledger) || ledger.length === 0) {
    ledger = [...SEED_FII_DII_HISTORY];
  }

  let liveSourceOk = false;
  let liveDateISO = null;

  if (Array.isArray(fiiRaw) && fiiRaw.length > 0) {
    const fiiRow = fiiRaw.find(r => String(r.category || '').toUpperCase().includes('FII'));
    const diiRow = fiiRaw.find(r => String(r.category || '').toUpperCase().includes('DII'));
    const rawDate = fiiRow?.date || diiRow?.date;
    const parsedIso = parseNseDateToISO(rawDate);

    if (parsedIso && fiiRow && diiRow) {
      const fiiNet = Number(parseFloat(fiiRow.netValue).toFixed(2));
      const diiNet = Number(parseFloat(diiRow.netValue).toFixed(2));
      const combined = Number((fiiNet + diiNet).toFixed(2));
      const label = rawDate.replace(/-/g, ' ');
      const entry = {
        date: parsedIso,
        label,
        fii: fiiNet,
        dii: diiNet,
        net: combined,
        fiiBuy: Number(parseFloat(fiiRow.buyValue).toFixed(2)),
        fiiSell: Number(parseFloat(fiiRow.sellValue).toFixed(2)),
        diiBuy: Number(parseFloat(diiRow.buyValue).toFixed(2)),
        diiSell: Number(parseFloat(diiRow.sellValue).toFixed(2)),
        note: 'Official NSE Daily Turnover',
        source: 'NSE India (fiidiiTradeReact)'
      };

      const existingIdx = ledger.findIndex(x => x.date === parsedIso);
      if (existingIdx >= 0) {
        ledger[existingIdx] = entry;
      } else {
        ledger.unshift(entry);
      }
      ledger.sort((a, b) => b.date.localeCompare(a.date));
      ledger = ledger.slice(0, 10);
      writeJsonAtomic(FII_LEDGER_FILE, ledger);
      liveSourceOk = true;
      liveDateISO = parsedIso;
    }
  }

  const latest = ledger[0] || null;
  return {
    asOf: latest ? sessionCloseISO(latest.date) : null,
    asOfDisplay: latest ? `${latest.label} (Provisional EOD)` : 'Unavailable',
    source: liveSourceOk
      ? 'NSE Cash Market Daily Turnover API + Historical Settlement'
      : 'NSE Cash Market Historical Ledger (Live NSE API unreachable)',
    liveFetchSucceeded: liveSourceOk,
    latestDateISO: liveDateISO || latest?.date || null,
    summary: latest
      ? {
          date: latest.date,
          label: latest.label,
          fiiNet: latest.fii,
          diiNet: latest.dii,
          combinedNet: latest.net
        }
      : null,
    sessions: ledger
  };
}

function buildBreadthSection(advanceRaw) {
  const cnt = advanceRaw?.advance?.count;
  if (!cnt || cnt.Total == null) {
    return {
      available: false,
      asOf: null,
      source: 'NSE India Market Breadth Feed (Unavailable)',
      advances: null,
      declines: null,
      unchanged: null,
      total: null
    };
  }
  const adv = Number(cnt.Advances || 0);
  const dec = Number(cnt.Declines || 0);
  const unc = Number(cnt.Unchange || 0);
  const total = Number(cnt.Total || (adv + dec + unc));
  return {
    available: true,
    asOf: advanceRaw.timestamp || formatISTDateTime(new Date()),
    source: 'NSE India Live Market Breadth Feed',
    advances: adv,
    declines: dec,
    unchanged: unc,
    total,
    advancesPct: total > 0 ? Number(((adv / total) * 100).toFixed(1)) : 0,
    declinesPct: total > 0 ? Number(((dec / total) * 100).toFixed(1)) : 0,
    unchangedPct: total > 0 ? Number(((unc / total) * 100).toFixed(1)) : 0
  };
}

function buildEarningsCalendarSection(eventsRaw, universe = loadStockUniverse()) {
  if (!Array.isArray(eventsRaw) || eventsRaw.length === 0) {
    return {
      available: false,
      asOf: formatISTDateTime(new Date()),
      source: 'NSE Corporate Event Calendar (Unavailable)',
      items: []
    };
  }
  const universeSet = new Set(universe.map(s => s.symbol));
  const finEvents = eventsRaw.filter(ev =>
    String(ev.purpose || '').toLowerCase().includes('result')
  );
  const prioritized = [
    ...finEvents.filter(ev => universeSet.has(ev.symbol)),
    ...finEvents.filter(ev => !universeSet.has(ev.symbol))
  ].slice(0, 15);

  return {
    available: true,
    asOf: formatISTDateTime(new Date()),
    source: 'NSE Corporate Event Calendar API',
    items: prioritized.map(ev => ({
      symbol: ev.symbol,
      company: ev.company,
      date: ev.date,
      purpose: ev.purpose,
      details: ev.bm_desc || ev.purpose,
      inUniverse: universeSet.has(ev.symbol)
    }))
  };
}

/**
 * Fetches real quarterly financials for top bellwether stocks in the active universe
 * so the Results tab remains populated without querying thousands of symbols unnecessarily.
 */
async function fetchQuarterlyFinancialResults(stockEntries) {
  const yf = await getYahooFinance();
  const bellwetherOrder = ['RELIANCE', 'TCS', 'HDFCBANK', 'ICICIBANK', 'INFY', 'BHARTIARTL', 'LT', 'SBIN', 'TRENT', 'BEL', 'HAL', 'DIXON'];
  const bySym = new Map(stockEntries.map(s => [s.symbol, s]));
  const targets = [];
  for (const sym of bellwetherOrder) {
    if (bySym.has(sym)) targets.push(bySym.get(sym));
  }
  if (targets.length < 12) {
    for (const s of stockEntries) {
      if (!targets.some(t => t.symbol === s.symbol)) {
        targets.push(s);
        if (targets.length >= 12) break;
      }
    }
  }

  const results = await runWithConcurrency(targets, 4, async (stock) => {
    try {
      const series = await yf.fundamentalsTimeSeries(stock.yahooSymbol, {
        period1: '2025-05-01',
        type: 'quarterly',
        module: 'financials'
      });
      if (!Array.isArray(series) || series.length === 0) return null;

      const sorted = series
        .filter(q => q.date && (q.totalRevenue != null || q.netIncome != null))
        .sort((a, b) => new Date(a.date) - new Date(b.date));

      if (sorted.length === 0) return null;
      const latestQ = sorted[sorted.length - 1];
      const prevQ = sorted.length >= 2 ? sorted[sorted.length - 2] : null;
      const latestDate = new Date(latestQ.date);

      const yoyQ = sorted.find(q => {
        const diffDays = Math.abs((latestDate - new Date(q.date)) / 86400000);
        return diffDays >= 340 && diffDays <= 390;
      });

      const revCr = latestQ.totalRevenue != null ? latestQ.totalRevenue / 1e7 : null;
      const patCr = latestQ.netIncome != null ? latestQ.netIncome / 1e7 : null;

      const revYoyPct = (yoyQ?.totalRevenue > 0 && latestQ.totalRevenue != null)
        ? Number((((latestQ.totalRevenue - yoyQ.totalRevenue) / yoyQ.totalRevenue) * 100).toFixed(1))
        : null;
      const revQoqPct = (prevQ?.totalRevenue > 0 && latestQ.totalRevenue != null)
        ? Number((((latestQ.totalRevenue - prevQ.totalRevenue) / prevQ.totalRevenue) * 100).toFixed(1))
        : null;

      const patYoyPct = (yoyQ?.netIncome && Math.abs(yoyQ.netIncome) > 0 && latestQ.netIncome != null)
        ? Number((((latestQ.netIncome - yoyQ.netIncome) / Math.abs(yoyQ.netIncome)) * 100).toFixed(1))
        : null;
      const patQoqPct = (prevQ?.netIncome && Math.abs(prevQ.netIncome) > 0 && latestQ.netIncome != null)
        ? Number((((latestQ.netIncome - prevQ.netIncome) / Math.abs(prevQ.netIncome)) * 100).toFixed(1))
        : null;

      let verdict = 'IN_LINE';
      if (patYoyPct != null && patYoyPct >= 20) verdict = 'STRONG_BEAT';
      else if (patYoyPct != null && patYoyPct < 0) verdict = 'WEAK_MISS';

      const periodEnd = latestDate.toISOString().slice(0, 10);
      return {
        symbol: stock.symbol,
        name: stock.name,
        sector: stock.sector,
        periodEnd,
        revCr: revCr != null ? Number(revCr.toFixed(1)) : null,
        revYoyPct,
        revQoqPct,
        patCr: patCr != null ? Number(patCr.toFixed(1)) : null,
        patYoyPct,
        patQoqPct,
        verdict,
        latestPrice: stock.price,
        chgPct: stock.chgPct,
        volRatio: stock.volRatio,
        asOf: periodEnd,
        source: 'Yahoo Finance Quarterly Financials'
      };
    } catch (e) {
      return null;
    }
  });

  return results.filter(Boolean);
}

function buildFactBackedBullets({ indices, stocks, fiiDii, breadth, sessionDate, universeStats }) {
  const bullets = [];
  const sessionCloseStr = `${sessionDate} 15:30 IST`;
  const nifty50 = indices.find(i => i.key === 'NIFTY_50');
  const nifty500 = indices.find(i => i.key === 'NIFTY_500');
  const bankNifty = indices.find(i => i.key === 'BANK_NIFTY');
  const midcap = indices.find(i => i.key === 'NIFTY_MIDCAP_150') || indices.find(i => i.key === 'NIFTY_MIDCAP_100');
  const smallcap = indices.find(i => i.key === 'NIFTY_SMALLCAP_250') || indices.find(i => i.key === 'NIFTY_SMALLCAP_100');

  if (nifty50 && nifty500) {
    bullets.push({
      category: 'Benchmark Close',
      tag: 'macro',
      title: `NIFTY 50 closed at ${nifty50.price.toLocaleString('en-IN')} (${nifty50.chgPct >= 0 ? '+' : ''}${nifty50.chgPct}%) • NIFTY 500 at ${nifty500.price.toLocaleString('en-IN')} (${nifty500.chgPct >= 0 ? '+' : ''}${nifty500.chgPct}%)`,
      desc: `Latest completed EOD settlement (${sessionDate}). NIFTY 50 is ${nifty50.price > nifty50.ema21 ? 'above' : 'below'} its 21 EMA (${nifty50.ema21.toLocaleString('en-IN')}) with RSI(14) at ${nifty50.rsi14}.`,
      asOf: sessionCloseStr,
      source: nifty50.source
    });
  }

  if (universeStats) {
    bullets.push({
      category: 'Universe Coverage',
      tag: 'macro',
      title: `Rule-Based NSE Screen: ${stocks.length.toLocaleString('en-IN')} Equities Loaded (MCap ≥ ₹${universeStats.minMcapCr} Cr)`,
      desc: `Tier breakdown — Large-Cap (1–100): ${universeStats.large}, Mid-Cap (101–250): ${universeStats.mid}, Small-Cap (251–500): ${universeStats.small}, Micro-Cap (501+): ${universeStats.micro}. Adjusted close prices used for all quant & RS returns.`,
      asOf: sessionCloseStr,
      source: 'NSE EQUITY_L.csv + Yahoo Finance EOD'
    });
  }

  if (fiiDii?.summary) {
    const s = fiiDii.summary;
    bullets.push({
      category: 'Institutional Flows',
      tag: 'institutional',
      title: `FII Net ${s.fiiNet >= 0 ? '+' : ''}₹${s.fiiNet.toLocaleString('en-IN')} Cr • DII Net ${s.diiNet >= 0 ? '+' : ''}₹${s.diiNet.toLocaleString('en-IN')} Cr (${s.label})`,
      desc: `Combined institutional cash flow stood at ${s.combinedNet >= 0 ? '+' : ''}₹${s.combinedNet.toLocaleString('en-IN')} Cr in the latest NSE turnover report.`,
      asOf: fiiDii.asOfDisplay,
      source: fiiDii.source
    });
  }

  if (breadth?.available) {
    bullets.push({
      category: 'Market Breadth',
      tag: 'breadth',
      title: `NSE Breadth: ${breadth.advances.toLocaleString()} Advances vs ${breadth.declines.toLocaleString()} Declines (${breadth.advancesPct}% Adv)`,
      desc: `${breadth.total.toLocaleString()} equities tracked (${breadth.unchanged} unchanged) on NSE capital market breadth feed.`,
      asOf: breadth.asOf,
      source: breadth.source
    });
  }

  if (bankNifty && midcap && smallcap) {
    const fmtChg = (idx) => idx.chgPct != null ? `${idx.chgPct >= 0 ? '+' : ''}${idx.chgPct}%` : `Close ${idx.price.toLocaleString('en-IN')}`;
    bullets.push({
      category: 'Broader Market',
      tag: 'sectoral',
      title: `${bankNifty.name} ${fmtChg(bankNifty)} • ${midcap.name} ${fmtChg(midcap)} • ${smallcap.name} ${fmtChg(smallcap)}`,
      desc: `Cross-cap EOD performance comparison: ${bankNifty.name} settled at ${bankNifty.price.toLocaleString('en-IN')}, ${midcap.name} at ${midcap.price.toLocaleString('en-IN')}, ${smallcap.name} at ${smallcap.price.toLocaleString('en-IN')}.`,
      asOf: sessionCloseStr,
      source: 'Yahoo Finance (NSE Indices EOD)'
    });
  }

  const topRs = [...stocks].sort((a, b) => b.rs - a.rs).slice(0, 3);
  if (topRs.length > 0) {
    bullets.push({
      category: 'RS Leaders',
      tag: 'sectoral',
      title: `Top Universe RS Leaders: ${topRs.map(s => `${s.symbol} (RS ${s.rs})`).join(', ')}`,
      desc: `Percentile ranked by weighted adjusted 3/6/9/12M returns (40/20/20/20%) across all ${stocks.length.toLocaleString('en-IN')} qualifying NSE equities.`,
      asOf: sessionCloseStr,
      source: 'Backend Quant Engine (Yahoo Finance EOD)'
    });
  }

  const topGainer = [...stocks].sort((a, b) => b.chgPct - a.chgPct)[0];
  const topLoser = [...stocks].sort((a, b) => a.chgPct - b.chgPct)[0];
  if (topGainer && topLoser) {
    bullets.push({
      category: 'Session Movers',
      tag: 'order',
      title: `Top Gainer: ${topGainer.symbol} (${topGainer.chgPct >= 0 ? '+' : ''}${topGainer.chgPct}%) • Weakest: ${topLoser.symbol} (${topLoser.chgPct >= 0 ? '+' : ''}${topLoser.chgPct}%)`,
      desc: `${topGainer.symbol} closed at ₹${topGainer.price.toLocaleString('en-IN')} (${topGainer.volRatio}x 20D avg vol); ${topLoser.symbol} closed at ₹${topLoser.price.toLocaleString('en-IN')}.`,
      asOf: sessionCloseStr,
      source: 'Yahoo Finance EOD'
    });
  }

  const sectorals = indices.filter(i => i.group === 'Sectoral').sort((a, b) => b.chgPct - a.chgPct);
  if (sectorals.length >= 2) {
    const best = sectorals[0];
    const worst = sectorals[sectorals.length - 1];
    bullets.push({
      category: 'Sector Rotation',
      tag: 'sectoral',
      title: `Leading Sector: ${best.name} (${best.chgPct >= 0 ? '+' : ''}${best.chgPct}%) • Lagging: ${worst.name} (${worst.chgPct >= 0 ? '+' : ''}${worst.chgPct}%)`,
      desc: `${best.name} settled at ${best.price.toLocaleString('en-IN')} while ${worst.name} closed at ${worst.price.toLocaleString('en-IN')}.`,
      asOf: sessionCloseStr,
      source: 'Yahoo Finance (NSE Indices EOD)'
    });
  }

  return bullets;
}

/**
 * Main loader for market data, full stock details, and batched history snapshots.
 * Returns `{ ...payload, _detailsBySymbol, _historyBySymbol }` (note: underscore fields are non-enumerable
 * or stripped before JSON API/market.json serialization so market.json stays compact).
 */
async function getMarketData(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && marketCache.data && (now - marketCache.fetchedAt) < CACHE_TTL_MS) {
    return {
      ...marketCache.data,
      meta: {
        ...marketCache.data.meta,
        cacheHit: true,
        cacheAgeSeconds: Math.round((now - marketCache.fetchedAt) / 1000)
      }
    };
  }

  if (inflightMarketPromise) {
    return inflightMarketPromise;
  }

  inflightMarketPromise = (async () => {
    try {
      const cfg = loadScreenerConfig();
      const expectedLatestSession = getLatestCompletedSessionDate(new Date());
      const yf = await getYahooFinance();

      // Stage 1 & 2: Two-stage rule-based NSE universe + sector cache
      const universeRes = await buildAndSaveUniverse(yf);
      const universe = universeRes.universe;
      const stage2QuoteMap = universeRes.quoteMap || new Map();

      // Fetch quotes for benchmark indices
      const idxQuoteMap = new Map();
      try {
        const idxQuotes = await yf.quote(INDEX_CATALOG.filter(i => !i.optional).map(i => i.yahooSymbol));
        for (const q of idxQuotes) {
          if (q && q.symbol) idxQuoteMap.set(q.symbol, q);
        }
      } catch (e) {
        console.warn('Index quote fetch partial failure:', e.message);
      }

      // Fetch Index EOD bars (including optional Nifty Microcap 250 probe)
      const indexResults = await runWithConcurrency(INDEX_CATALOG, 4, async (idxMeta) => {
        if (!idxMeta.hasFullHistory) {
          const q = idxQuoteMap.get(idxMeta.yahooSymbol);
          const marketTimeDate = q?.regularMarketTime ? toISTDateStr(q.regularMarketTime) : null;
          const isNewerLiveSession = Boolean(marketTimeDate && marketTimeDate > expectedLatestSession && q?.regularMarketPreviousClose);
          const eodPrice = isNewerLiveSession
            ? Number(q.regularMarketPreviousClose.toFixed(2))
            : (q?.regularMarketPrice ? Number(q.regularMarketPrice.toFixed(2)) : null);

          if (eodPrice == null) return null;

          const eodPrevClose = !isNewerLiveSession && q?.regularMarketPreviousClose != null
            ? Number(q.regularMarketPreviousClose.toFixed(2))
            : null;
          const eodChg = !isNewerLiveSession && q?.regularMarketChange != null
            ? Number(q.regularMarketChange.toFixed(2))
            : null;
          const eodChgPct = !isNewerLiveSession && q?.regularMarketChangePercent != null
            ? Number(q.regularMarketChangePercent.toFixed(2))
            : null;

          return {
            key: idxMeta.key,
            name: idxMeta.name,
            yahooSymbol: idxMeta.yahooSymbol,
            group: idxMeta.group,
            sessionDate: expectedLatestSession,
            asOf: sessionCloseISO(expectedLatestSession),
            source: 'Yahoo Finance (NSE Index EOD Quote)',
            price: eodPrice,
            prevClose: eodPrevClose,
            chg: eodChg,
            chgPct: eodChgPct,
            high52: q?.fiftyTwoWeekHigh ? Number(q.fiftyTwoWeekHigh.toFixed(2)) : null,
            low52: q?.fiftyTwoWeekLow ? Number(q.fiftyTwoWeekLow.toFixed(2)) : null,
            ema21: null,
            sma50: null,
            sma200: null,
            rsi14: null,
            trend: 'EOD Quote (1Y series via Midcap 150 / Smallcap 250)',
            bars: []
          };
        }

        try {
          const { bars, meta } = await fetchDailyEodBars(idxMeta.yahooSymbol, expectedLatestSession, cfg.MAX_RETRIES, 500);
          if (!bars || bars.length < 2) {
            if (idxMeta.optional) return null;
            throw new Error(`Insufficient EOD bars for index ${idxMeta.yahooSymbol}`);
          }
          const tech = computeTechnicals(bars);
          const latestBar = bars[bars.length - 1];
          const q = idxQuoteMap.get(idxMeta.yahooSymbol);
          const trendLabel = tech.price > tech.ema21 && tech.ema21 > tech.sma50
            ? 'Above 21 EMA & 50 SMA'
            : tech.price > tech.sma200
              ? 'Above 200 SMA'
              : 'Below Major MAs';

          return {
            key: idxMeta.key,
            name: idxMeta.name,
            yahooSymbol: idxMeta.yahooSymbol,
            group: idxMeta.group,
            sessionDate: latestBar.date,
            asOf: sessionCloseISO(latestBar.date),
            source: 'Yahoo Finance (NSE Indices EOD)',
            price: tech.price,
            prevClose: tech.prevClose,
            chg: tech.chg,
            chgPct: tech.chgPct,
            high52: Number((q?.fiftyTwoWeekHigh || meta?.fiftyTwoWeekHigh || tech.high52).toFixed(2)),
            low52: Number((q?.fiftyTwoWeekLow || meta?.fiftyTwoWeekLow || tech.low52).toFixed(2)),
            ema21: tech.ema21,
            sma50: tech.sma50,
            sma200: tech.sma200,
            rsi14: tech.rsi14,
            trend: trendLabel,
            bars
          };
        } catch (err) {
          if (idxMeta.optional) {
            console.log(`Optional index ${idxMeta.name} (${idxMeta.yahooSymbol}) unavailable; falling back cleanly.`);
            return null;
          }
          throw err;
        }
      });

      const indexBarsByKey = new Map();
      const indicesPublic = indexResults.filter(Boolean).map(({ bars, ...rest }) => {
        if (bars && bars.length > 0) indexBarsByKey.set(rest.key, bars);
        return rest;
      });

      const nifty50Bars = indexBarsByKey.get('NIFTY_50');
      const n500Bars = indexBarsByKey.get('NIFTY_500');
      const microBars = indexBarsByKey.get('NIFTY_MICROCAP_250');
      const hasMicrocapIndex = Boolean(microBars && microBars.length >= cfg.MIN_BARS);

      if (!nifty50Bars || !n500Bars) {
        throw new Error('Core Nifty 50 / Nifty 500 benchmark bars unavailable.');
      }

      const latestIndexDate = nifty50Bars[nifty50Bars.length - 1].date;

      // Persist Nifty 500 trimmed history for RS charts
      const n500HistPayload = {
        symbol: '^CRSLDX',
        yahooSymbol: '^CRSLDX',
        name: 'NIFTY 500',
        asOf: sessionCloseISO(n500Bars[n500Bars.length - 1].date),
        asOfDisplay: formatISTDateTime(sessionCloseISO(n500Bars[n500Bars.length - 1].date)),
        source: 'Yahoo Finance Daily OHLCV',
        bars: n500Bars.slice(-(cfg.HISTORY_BARS_OUTPUT || 300))
      };
      historyCache.set('^CRSLDX', { data: n500HistPayload, fetchedAt: Date.now() });
      writeJsonAtomic(path.join(HISTORY_DIR, `${sanitizeSymbolFilename('^CRSLDX')}.json`), n500HistPayload);

      // Stage 3: Fetch daily OHLCV history in batches of 15 with 1s pause and 2 retries
      const failedList = [];
      const skippedList = [];
      const detailsBySymbol = new Map();
      const historyBySymbol = new Map();
      historyBySymbol.set('^CRSLDX', n500HistPayload);

      const rawStockResults = await processInBatches(
        universe,
        cfg.BATCH_SIZE || 15,
        cfg.BATCH_PAUSE_MS ?? 1000,
        async (stockMeta) => {
          try {
            const { bars } = await fetchDailyEodBars(
              stockMeta.yahooSymbol,
              expectedLatestSession,
              cfg.MAX_RETRIES ?? 2,
              500
            );

            // Quality check 1: minimum bars (default 252)
            if (!bars || bars.length < cfg.MIN_BARS) {
              const reason = `Insufficient bars (${bars ? bars.length : 0} < ${cfg.MIN_BARS})`;
              skippedList.push({ symbol: stockMeta.symbol, reason });
              return null;
            }

            // Quality check 2: <= 10% zero-volume days in the last MIN_BARS sessions
            const recentWindow = bars.slice(-cfg.MIN_BARS);
            const zeroVolDays = recentWindow.filter(b => !b.v || b.v <= 0).length;
            const zeroVolPct = (zeroVolDays / cfg.MIN_BARS) * 100;
            if (zeroVolPct > (cfg.MAX_ZERO_VOL_PCT ?? 10)) {
              const reason = `Zero-volume days ${zeroVolDays}/${cfg.MIN_BARS} (${zeroVolPct.toFixed(1)}% > ${cfg.MAX_ZERO_VOL_PCT}%)`;
              skippedList.push({ symbol: stockMeta.symbol, reason });
              return null;
            }

            // Persist last 300 bars to data/history/<SYM>.json
            const trimmedBars = bars.slice(-(cfg.HISTORY_BARS_OUTPUT || 300));
            const latestBarDate = trimmedBars[trimmedBars.length - 1].date;
            const histPayload = {
              symbol: stockMeta.symbol,
              yahooSymbol: stockMeta.yahooSymbol,
              name: stockMeta.name,
              asOf: sessionCloseISO(latestBarDate),
              asOfDisplay: formatISTDateTime(sessionCloseISO(latestBarDate)),
              source: 'Yahoo Finance Daily OHLCV',
              bars: trimmedBars
            };
            historyCache.set(stockMeta.symbol, { data: histPayload, fetchedAt: Date.now() });
            historyBySymbol.set(stockMeta.symbol, histPayload);
            writeJsonAtomic(path.join(HISTORY_DIR, `${sanitizeSymbolFilename(stockMeta.symbol)}.json`), histPayload);

            // Compute technicals (ret3M/6M/9M/12M use adjusted close!) & adjusted-close quant matrices
            const tech = computeTechnicals(bars);
            const q = stage2QuoteMap.get(stockMeta.yahooSymbol);
            const mcapCr = stockMeta.marketCapCr != null
              ? Math.round(stockMeta.marketCapCr)
              : (q?.marketCap ? Math.round(q.marketCap / 1e7) : null);
            const cap = stockMeta.cap || stockMeta.defaultCap;
            const bmInfo = benchmarkForCap(cap, hasMicrocapIndex);
            const relevantBmBars = indexBarsByKey.get(bmInfo.key) || nifty50Bars;

            const quantRelevant = computeBenchmarkComparison(bars, relevantBmBars);
            const quantNifty500 = computeBenchmarkComparison(bars, n500Bars);

            const high52 = Number((stockMeta.high52 || q?.fiftyTwoWeekHigh || tech.high52).toFixed(2));
            const low52 = Number((stockMeta.low52 || q?.fiftyTwoWeekLow || tech.low52).toFixed(2));
            const dist52Pct = high52 > 0 ? Number((Math.abs((tech.price - high52) / high52) * 100).toFixed(2)) : 0;

            return {
              symbol: stockMeta.symbol,
              yahooSymbol: stockMeta.yahooSymbol,
              name: stockMeta.name,
              sector: stockMeta.sector || 'Unclassified',
              cap,
              rank: stockMeta.rank ?? null,
              marketCapCr: mcapCr,
              avgTradedValCr: stockMeta.avgTradedValCr != null ? Number(stockMeta.avgTradedValCr.toFixed(2)) : null,
              benchmarkName: bmInfo.name,
              benchmarkKey: bmInfo.key,
              sessionDate: latestBarDate,
              asOf: sessionCloseISO(latestBarDate),
              source: 'Yahoo Finance (NSE EOD)',
              price: tech.price,
              prevClose: tech.prevClose,
              chg: tech.chg,
              chgPct: tech.chgPct,
              isUpper: tech.isUpper,
              high52,
              low52,
              dist52Pct,
              volRatio: tech.volRatio,
              latestVolume: tech.latestVolume,
              barsCount: bars.length,
              zeroVolDays252: zeroVolDays,
              ema21: tech.ema21,
              sma50: tech.sma50,
              sma200: tech.sma200,
              rsi14: tech.rsi14,
              macd: tech.macd,
              macdSig: tech.macdSig,
              macdHist: tech.macdHist,
              ret3M: tech.ret3M,
              ret6M: tech.ret6M,
              ret9M: tech.ret9M,
              ret12M: tech.ret12M,
              quant: {
                relevantBenchmark: bmInfo.name,
                relevant: quantRelevant,
                nifty500: quantNifty500
              }
            };
          } catch (stockErr) {
            console.warn(`Failed ${stockMeta.symbol} (${stockMeta.yahooSymbol}): ${stockErr.message}`);
            failedList.push({ symbol: stockMeta.symbol, reason: stockErr.message });
            return null;
          }
        }
      );

      const fullStockEntries = rawStockResults.filter(Boolean);
      if (fullStockEntries.length === 0) {
        throw new Error('All universe stocks failed or were skipped.');
      }

      // Compute RS rank percentile across the entire final valid universe
      assignUniverseRSPercentiles(fullStockEntries);

      // Persist full per-stock quant detail files (data/stocks/<SYM>.json) & build compact market.json rows
      const compactStocks = [];
      const tierCounts = { large: 0, mid: 0, small: 0, micro: 0 };

      for (const s of fullStockEntries) {
        if (s.cap === 'Large-Cap') tierCounts.large++;
        else if (s.cap === 'Mid-Cap') tierCounts.mid++;
        else if (s.cap === 'Small-Cap') tierCounts.small++;
        else tierCounts.micro++;

        detailsBySymbol.set(s.symbol, s);
        stockDetailCache.set(s.symbol, { data: s, fetchedAt: Date.now() });
        writeJsonAtomic(path.join(STOCKS_DIR, `${sanitizeSymbolFilename(s.symbol)}.json`), s);

        compactStocks.push({
          symbol: s.symbol,
          name: s.name,
          sector: s.sector,
          cap: s.cap,
          rank: s.rank,
          marketCapCr: s.marketCapCr,
          price: s.price,
          chgPct: s.chgPct,
          rs: s.rs,
          dist52Pct: s.dist52Pct,
          above50Sma: s.price != null && s.sma50 != null ? s.price > s.sma50 : false,
          above200Sma: s.price != null && s.sma200 != null ? s.price > s.sma200 : false,
          alpha3M: s.quant?.relevant?.['3M']?.alpha ?? null,
          volRatio: s.volRatio,
          sd1Y: s.quant?.relevant?.['1Y']?.sdStock ?? null,
          beta1Y: s.quant?.relevant?.['1Y']?.beta ?? null,
          benchmarkName: s.benchmarkName,
          asOf: s.asOf,
          source: s.source
        });
      }

      // Fetch NSE Supplementary Data + Bellwether Quarterly Financials concurrently
      const [nseData, quarterlyResults] = await Promise.all([
        fetchNseSupplementaryData(),
        fetchQuarterlyFinancialResults(fullStockEntries)
      ]);

      const fiiDii = mergeFiiDiiLedger(nseData.fiiRaw);
      const breadth = buildBreadthSection(nseData.advanceRaw);
      const earningsRadar = buildEarningsCalendarSection(nseData.eventsRaw, universe);
      const bullets = buildFactBackedBullets({
        indices: indicesPublic,
        stocks: compactStocks,
        fiiDii,
        breadth,
        sessionDate: latestIndexDate,
        universeStats: {
          ...tierCounts,
          minMcapCr: cfg.MIN_MCAP_CR
        }
      });

      const tradingDaysOld = countTradingDaysBetween(latestIndexDate, expectedLatestSession);
      const isStale = tradingDaysOld > 1;
      const generatedAt = new Date().toISOString();
      const microBmUsed = hasMicrocapIndex ? 'NIFTY MICROCAP 250' : 'NIFTY SMALLCAP 250';

      const payload = {
        meta: {
          asOf: sessionCloseISO(latestIndexDate),
          asOfDisplay: formatISTDateTime(sessionCloseISO(latestIndexDate)),
          generatedAt,
          generatedAtIST: formatISTDateTime(generatedAt),
          sessionDate: latestIndexDate,
          expectedSessionDate: expectedLatestSession,
          tradingDaysOld,
          isStale,
          fromSnapshot: false,
          cacheHit: false,
          universeStage1Count: universeRes.stage1Count,
          universeFullCount: universeRes.fullUniverseCount,
          universeTotal: universe.length,
          universeCount: compactStocks.length,
          capTiers: tierCounts,
          microcapBenchmarkUsed: microBmUsed,
          skippedSymbols: skippedList.map(x => x.symbol),
          skippedDetails: skippedList,
          failedSymbols: failedList.map(x => x.symbol),
          failedDetails: failedList,
          reusedSavedUniverse: Boolean(universeRes.reusedSavedUniverse),
          maxStocksApplied: universeRes.maxStocksApplied,
          configUsed: {
            MIN_MCAP_CR: cfg.MIN_MCAP_CR,
            MIN_AVG_TRADED_VALUE_CR: cfg.MIN_AVG_TRADED_VALUE_CR,
            MIN_BARS: cfg.MIN_BARS,
            MAX_ZERO_VOL_PCT: cfg.MAX_ZERO_VOL_PCT
          },
          source: 'NSE EQUITY_L.csv + Yahoo Finance EOD (yahoo-finance2) + NSE India API'
        },
        indices: indicesPublic,
        stocks: compactStocks,
        fiiDii,
        breadth,
        bullets,
        results: {
          asOf: formatISTDateTime(generatedAt),
          source: 'Yahoo Finance Quarterly Financials',
          items: quarterlyResults
        },
        earningsRadar
      };

      // Cache in memory and persist compact market snapshot to disk
      marketCache = { data: payload, fetchedAt: Date.now() };
      writeJsonAtomic(SNAPSHOT_FILE, payload);

      // Attach non-enumerable maps so build-static-data.js can write web/data/stocks/ & web/data/history/ directly
      Object.defineProperty(payload, '_detailsBySymbol', { value: detailsBySymbol, enumerable: false });
      Object.defineProperty(payload, '_historyBySymbol', { value: historyBySymbol, enumerable: false });

      return payload;
    } catch (liveErr) {
      console.error('Live market fetch failed:', liveErr.message);

      const snapshot = readJsonSafe(SNAPSHOT_FILE);
      if (snapshot && snapshot.meta && snapshot.meta.sessionDate) {
        const expectedLatestSession = getLatestCompletedSessionDate(new Date());
        const tradingDaysOld = countTradingDaysBetween(snapshot.meta.sessionDate, expectedLatestSession);
        const isStale = tradingDaysOld > 1;
        return {
          ...snapshot,
          meta: {
            ...snapshot.meta,
            expectedSessionDate: expectedLatestSession,
            tradingDaysOld,
            isStale,
            fromSnapshot: true,
            cacheHit: false,
            liveError: liveErr.message
          }
        };
      }

      throw liveErr;
    } finally {
      inflightMarketPromise = null;
    }
  })();

  return inflightMarketPromise;
}

/**
 * Fetches full quant detail for /api/stock/:symbol (15m memory cache + disk snapshot).
 */
async function getStockDetail(rawSymbol) {
  const clean = String(rawSymbol || '').trim().toUpperCase();
  if (!clean) throw new Error('Symbol parameter is required.');

  const now = Date.now();
  const cached = stockDetailCache.get(clean);
  if (cached && (now - cached.fetchedAt) < CACHE_TTL_MS) {
    return cached.data;
  }

  const diskFile = path.join(STOCKS_DIR, `${sanitizeSymbolFilename(clean)}.json`);
  const disk = readJsonSafe(diskFile);
  if (disk) {
    stockDetailCache.set(clean, { data: disk, fetchedAt: now });
    return disk;
  }

  throw new Error(`Stock detail not found for ${clean}`);
}

/**
 * Fetches real daily OHLCV history (last 300 bars) for /api/history/:symbol.
 */
async function getSymbolHistory(rawSymbol) {
  const clean = String(rawSymbol || '').trim().toUpperCase();
  if (!clean) {
    throw new Error('Symbol parameter is required.');
  }

  const universe = loadStockUniverse();
  const stockMeta = universe.find(s => s.symbol === clean || s.yahooSymbol.toUpperCase() === clean);
  const indexMeta = INDEX_CATALOG.find(i => i.key === clean || i.yahooSymbol.toUpperCase() === clean);

  const yahooSymbol = stockMeta ? stockMeta.yahooSymbol : (indexMeta ? indexMeta.yahooSymbol : (clean.includes('.') || clean.startsWith('^') ? clean : `${clean}.NS`));
  const displayName = stockMeta ? stockMeta.name : (indexMeta ? indexMeta.name : clean);
  const cacheKey = stockMeta ? stockMeta.symbol : yahooSymbol;

  const now = Date.now();
  const cached = historyCache.get(cacheKey);
  if (cached && (now - cached.fetchedAt) < CACHE_TTL_MS) {
    return { ...cached.data, cacheHit: true };
  }

  const cfg = loadScreenerConfig();
  const diskFile = path.join(HISTORY_DIR, `${sanitizeSymbolFilename(cacheKey)}.json`);
  try {
    const expectedLatestSession = getLatestCompletedSessionDate(new Date());
    const { bars } = await fetchDailyEodBars(yahooSymbol, expectedLatestSession, cfg.MAX_RETRIES ?? 2, 500);
    if (!bars || bars.length === 0) {
      throw new Error(`No historical bars returned for ${yahooSymbol}`);
    }

    const trimmedBars = bars.slice(-(cfg.HISTORY_BARS_OUTPUT || 300));
    const latestDate = trimmedBars[trimmedBars.length - 1].date;
    const payload = {
      symbol: cacheKey,
      yahooSymbol,
      name: displayName,
      asOf: sessionCloseISO(latestDate),
      asOfDisplay: formatISTDateTime(sessionCloseISO(latestDate)),
      source: 'Yahoo Finance Daily OHLCV',
      bars: trimmedBars
    };

    historyCache.set(cacheKey, { data: payload, fetchedAt: now });
    writeJsonAtomic(diskFile, payload);
    return { ...payload, cacheHit: false };
  } catch (err) {
    const disk = readJsonSafe(diskFile);
    if (disk && Array.isArray(disk.bars) && disk.bars.length > 0) {
      return { ...disk, fromSnapshot: true, liveError: err.message };
    }
    throw err;
  }
}

module.exports = {
  INDEX_CATALOG,
  loadStockUniverse,
  loadScreenerConfig,
  getMarketData,
  getStockDetail,
  getSymbolHistory
};
