/**
 * lib/universeBuilder.js
 * Two-stage rule-based NSE equity universe builder:
 *   Stage 1: Official NSE EQUITY_L.csv (series EQ and BE only; excludes SME, ETFs, REITs, InvITs, Prefs, Rights).
 *   Stage 2: Batch Yahoo Finance quotes (marketCap >= MIN_MCAP_CR and 3M avg traded value >= MIN_AVG_TRADED_VALUE_CR).
 *   Stage 3: Nifty 500 Industry CSV + monthly cached Yahoo assetProfile sector -> saved to config/universe.csv.
 *
 * Fallback policy: On download/quote failure, reuses the last saved config/universe.csv.
 * Never falls back to any hand-picked stock list.
 */

const fs = require('fs');
const path = require('path');

const CONFIG_FILE = path.join(__dirname, '..', 'config', 'screener-config.json');
const UNIVERSE_CSV_FILE = path.join(__dirname, '..', 'config', 'universe.csv');

const NSE_EQUITY_LIST_URL = 'https://nsearchives.nseindia.com/content/equities/EQUITY_L.csv';
const NIFTY_500_LIST_URL = 'https://nsearchives.nseindia.com/content/indices/ind_nifty500list.csv';

const DEFAULT_CONFIG = {
  MIN_MCAP_CR: 1000,
  MIN_AVG_TRADED_VALUE_CR: 1,
  MIN_BARS: 252,
  MAX_ZERO_VOL_PCT: 10,
  SECTOR_CACHE_TTL_DAYS: 30,
  HISTORY_BARS_OUTPUT: 300,
  BATCH_SIZE: 15,
  BATCH_PAUSE_MS: 1000,
  MAX_RETRIES: 2
};

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

function loadScreenerConfig(configPath = CONFIG_FILE) {
  try {
    if (fs.existsSync(configPath)) {
      const parsed = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
      return { ...DEFAULT_CONFIG, ...parsed };
    }
  } catch (err) {
    console.warn(`Warning reading ${configPath}, using defaults: ${err.message}`);
  }
  return { ...DEFAULT_CONFIG };
}

function parseCsvLine(line) {
  const fields = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"' && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        cur += ch;
      }
    } else if (ch === '"') {
      inQuotes = true;
    } else if (ch === ',') {
      fields.push(cur.trim());
      cur = '';
    } else {
      cur += ch;
    }
  }
  fields.push(cur.trim());
  return fields;
}

function csvEscape(val) {
  const str = String(val ?? '');
  if (/[",\r\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

function writeFileAtomic(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const tmp = `${filePath}.tmp`;
  fs.writeFileSync(tmp, content, 'utf-8');
  fs.renameSync(tmp, filePath);
}

function classifyCapByRank(rank) {
  if (rank <= 100) return 'Large-Cap';
  if (rank <= 250) return 'Mid-Cap';
  if (rank <= 500) return 'Small-Cap';
  return 'Micro-Cap';
}

function isExcludedInstrument(symbol, name, series) {
  const ser = String(series || '').trim().toUpperCase();
  if (ser !== 'EQ' && ser !== 'BE') return true;

  const sym = String(symbol || '').trim().toUpperCase();
  const nm = String(name || '').trim().toUpperCase();
  if (!sym) return true;

  // Exclude rights entitlements (-RE, -RE1, etc.), preference shares, SME, ETFs, REITs, InvITs
  if (/-RE\d*$/i.test(sym)) return true;
  if (/\b(ETF|BEES|REIT|INVIT|PREF|PREFERENCE|CCPS|OCRPS|NCD|DEBENTURE|BOND|SME)\b/i.test(`${nm} ${sym}`)) {
    return true;
  }
  return false;
}

/**
 * Reads cached universe from config/universe.csv (supports both compact 5-col and extended 9-col schema).
 */
function loadStockUniverse(csvPath = UNIVERSE_CSV_FILE) {
  if (!fs.existsSync(csvPath)) {
    return [];
  }
  const raw = fs.readFileSync(csvPath, 'utf-8');
  const lines = raw
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l && !l.startsWith('#'));

  if (lines.length < 2) return [];

  const headers = parseCsvLine(lines[0]).map(h => h.toLowerCase().trim());
  const idxSym = headers.indexOf('symbol');
  const idxName = headers.indexOf('name');
  const idxSector = headers.indexOf('sector');
  const idxCap = headers.indexOf('cap');
  const idxYahoo = headers.indexOf('yahoo_symbol');
  const idxMcap = headers.indexOf('market_cap_cr');
  const idxRank = headers.indexOf('rank');
  const idxVal = headers.indexOf('avg_traded_val_cr');
  const idxSecDate = headers.indexOf('sector_updated_at');

  if (idxSym < 0 || idxName < 0 || idxSector < 0 || idxCap < 0 || idxYahoo < 0) {
    throw new Error('universe.csv must have columns: symbol,name,sector,cap,yahoo_symbol');
  }

  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    const symbol = (cols[idxSym] || '').toUpperCase();
    const yahooSymbol = cols[idxYahoo] || '';
    if (!symbol || !yahooSymbol) continue;

    const mcapVal = idxMcap >= 0 && cols[idxMcap] ? Number(cols[idxMcap]) : null;
    const rankVal = idxRank >= 0 && cols[idxRank] ? Number(cols[idxRank]) : i;
    const avgVal = idxVal >= 0 && cols[idxVal] ? Number(cols[idxVal]) : null;
    const sectorUpdatedAt = idxSecDate >= 0 ? (cols[idxSecDate] || '') : '';

    rows.push({
      symbol,
      name: cols[idxName] || symbol,
      sector: cols[idxSector] || 'Unclassified',
      cap: cols[idxCap] || classifyCapByRank(rankVal),
      defaultCap: cols[idxCap] || classifyCapByRank(rankVal),
      yahooSymbol,
      marketCapCr: Number.isFinite(mcapVal) ? mcapVal : null,
      rank: Number.isFinite(rankVal) ? rankVal : i,
      avgTradedValCr: Number.isFinite(avgVal) ? avgVal : null,
      sectorUpdatedAt
    });
  }
  return rows;
}

function saveUniverseCsv(rows, csvPath = UNIVERSE_CSV_FILE) {
  const header = 'symbol,name,sector,cap,yahoo_symbol,market_cap_cr,rank,avg_traded_val_cr,sector_updated_at';
  const lines = [header];
  for (const r of rows) {
    lines.push([
      csvEscape(r.symbol),
      csvEscape(r.name),
      csvEscape(r.sector || 'Unclassified'),
      csvEscape(r.cap),
      csvEscape(r.yahooSymbol),
      csvEscape(r.marketCapCr != null ? Math.round(r.marketCapCr) : ''),
      csvEscape(r.rank ?? ''),
      csvEscape(r.avgTradedValCr != null ? Number(r.avgTradedValCr.toFixed(2)) : ''),
      csvEscape(r.sectorUpdatedAt || '')
    ].join(','));
  }
  writeFileAtomic(csvPath, lines.join('\n') + '\n');
}

async function fetchTextWithRetry(url, retries = 2, timeoutMs = 15000) {
  const ua = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  let lastErr = null;
  for (let attempt = 0; attempt <= retries; attempt++) {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
      const resp = await fetch(url, {
        headers: {
          'User-Agent': ua,
          'Accept': 'text/csv,text/plain,*/*'
        },
        signal: ctrl.signal
      });
      clearTimeout(timer);
      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status} for ${url}`);
      }
      return await resp.text();
    } catch (err) {
      clearTimeout(timer);
      lastErr = err;
      if (attempt < retries) await sleep(800 * (attempt + 1));
    }
  }
  throw lastErr;
}

async function fetchNseEquityList() {
  const csvText = await fetchTextWithRetry(NSE_EQUITY_LIST_URL, 2, 20000);
  const lines = csvText.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
  if (lines.length < 10) {
    throw new Error('NSE EQUITY_L.csv returned unexpectedly few rows');
  }

  const headers = parseCsvLine(lines[0]).map(h => h.trim().toUpperCase());
  const idxSym = headers.indexOf('SYMBOL');
  const idxName = headers.findIndex(h => h.includes('NAME'));
  const idxSer = headers.indexOf('SERIES');

  if (idxSym < 0 || idxName < 0 || idxSer < 0) {
    throw new Error(`Unexpected EQUITY_L.csv header: ${lines[0]}`);
  }

  const stage1 = [];
  const seen = new Set();
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    const symbol = (cols[idxSym] || '').trim().toUpperCase();
    const name = (cols[idxName] || symbol).trim();
    const series = (cols[idxSer] || '').trim().toUpperCase();

    if (isExcludedInstrument(symbol, name, series)) continue;
    if (seen.has(symbol)) continue;
    seen.add(symbol);

    stage1.push({
      symbol,
      name,
      series,
      yahooSymbol: `${symbol}.NS`
    });
  }
  return stage1;
}

async function fetchNifty500IndustryMap() {
  const map = new Map();
  try {
    const csvText = await fetchTextWithRetry(NIFTY_500_LIST_URL, 1, 12000);
    const lines = csvText.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    if (lines.length < 2) return map;

    const headers = parseCsvLine(lines[0]).map(h => h.trim().toUpperCase());
    const idxSym = headers.indexOf('SYMBOL');
    const idxInd = headers.indexOf('INDUSTRY');
    if (idxSym < 0 || idxInd < 0) return map;

    for (let i = 1; i < lines.length; i++) {
      const cols = parseCsvLine(lines[i]);
      const sym = (cols[idxSym] || '').trim().toUpperCase();
      const ind = (cols[idxInd] || '').trim();
      if (sym && ind) {
        map.set(sym, ind);
      }
    }
  } catch (err) {
    console.warn(`Nifty 500 industry CSV unavailable (${err.message}); falling back to Yahoo/cache sectors.`);
  }
  return map;
}

function isSectorCacheFresh(updatedAtStr, ttlDays) {
  if (!updatedAtStr) return false;
  const dt = new Date(updatedAtStr);
  if (Number.isNaN(dt.getTime())) return false;
  const ageDays = (Date.now() - dt.getTime()) / (86400 * 1000);
  return ageDays <= ttlDays;
}

async function fetchBatchQuotes(yf, yahooSymbols, chunkSize = 150, retries = 2) {
  const quoteMap = new Map();
  for (let i = 0; i < yahooSymbols.length; i += chunkSize) {
    const chunk = yahooSymbols.slice(i, i + chunkSize);
    let attempt = 0;
    let done = false;
    while (attempt <= retries && !done) {
      try {
        const res = await yf.quote(chunk);
        if (Array.isArray(res)) {
          for (const q of res) {
            if (q && q.symbol) quoteMap.set(q.symbol, q);
          }
        }
        done = true;
      } catch (err) {
        attempt++;
        if (attempt > retries) {
          console.warn(`Batch quote chunk ${i}-${i + chunk.length} failed after retries: ${err.message}`);
        } else {
          await sleep(500 * attempt);
        }
      }
    }
  }
  return quoteMap;
}

async function resolveSectorsForRows(yf, rows, nifty500Map, cachedBySym, cfg, onlyIndicesSet = null) {
  const todayStr = new Date().toISOString().slice(0, 10);
  const needYahooIndices = [];

  for (let i = 0; i < rows.length; i++) {
    const r = rows[i];
    const n500Sector = nifty500Map.get(r.symbol);
    if (n500Sector) {
      r.sector = n500Sector;
      r.sectorUpdatedAt = todayStr;
      continue;
    }

    const cached = cachedBySym.get(r.symbol);
    if (
      cached &&
      cached.sector &&
      cached.sector !== 'Unclassified' &&
      isSectorCacheFresh(cached.sectorUpdatedAt, cfg.SECTOR_CACHE_TTL_DAYS)
    ) {
      r.sector = cached.sector;
      r.sectorUpdatedAt = cached.sectorUpdatedAt;
      continue;
    }

    // If cached sector exists even if stale, prefill as fallback before trying refresh
    if (cached && cached.sector && cached.sector !== 'Unclassified') {
      r.sector = cached.sector;
      r.sectorUpdatedAt = cached.sectorUpdatedAt || todayStr;
    } else {
      r.sector = 'Unclassified';
      r.sectorUpdatedAt = todayStr;
    }

    if (!onlyIndicesSet || onlyIndicesSet.has(i)) {
      needYahooIndices.push(i);
    }
  }

  if (needYahooIndices.length > 0) {
    console.log(`Fetching Yahoo sectors for ${needYahooIndices.length} non-Nifty-500 equities...`);
    const batchSize = cfg.BATCH_SIZE || 15;
    for (let start = 0; start < needYahooIndices.length; start += batchSize) {
      const batchIdxs = needYahooIndices.slice(start, start + batchSize);
      await Promise.all(
        batchIdxs.map(async (rowIdx) => {
          const r = rows[rowIdx];
          for (let attempt = 0; attempt <= 1; attempt++) {
            try {
              const qs = await yf.quoteSummary(r.yahooSymbol, { modules: ['assetProfile'] });
              const ap = qs?.assetProfile;
              const sec = (ap?.industry || ap?.sector || '').trim();
              if (sec) {
                r.sector = sec;
              }
              r.sectorUpdatedAt = todayStr;
              return;
            } catch (e) {
              if (attempt === 0) await sleep(300);
            }
          }
        })
      );
      if (start + batchSize < needYahooIndices.length) {
        await sleep(300);
      }
    }
  }
}

/**
 * Runs Stage 1 + Stage 2 + Sector caching and persists config/universe.csv.
 * Reuses last saved config/universe.csv on failure. Applies MAX_STOCKS if set.
 */
async function buildAndSaveUniverse(yf) {
  const cfg = loadScreenerConfig();
  const maxStocksEnv = Number(process.env.MAX_STOCKS);
  const maxStocks = Number.isFinite(maxStocksEnv) && maxStocksEnv > 0 ? Math.floor(maxStocksEnv) : null;

  const cachedRows = loadStockUniverse(UNIVERSE_CSV_FILE);
  const cachedBySym = new Map(cachedRows.map(r => [r.symbol, r]));

  try {
    // Stage 1: NSE EQUITY_L.csv
    const stage1 = await fetchNseEquityList();

    // Stage 2: Yahoo Finance batch quotes for MCap & 3M average traded value
    const [quoteMap, nifty500Map] = await Promise.all([
      fetchBatchQuotes(yf, stage1.map(s => s.yahooSymbol), 150, 2),
      fetchNifty500IndustryMap()
    ]);

    if (quoteMap.size === 0) {
      throw new Error('Yahoo batch quote returned 0 quotes');
    }

    const qualifying = [];
    for (const item of stage1) {
      const q = quoteMap.get(item.yahooSymbol);
      if (!q) continue;
      if (q.quoteType && String(q.quoteType).toUpperCase() !== 'EQUITY') continue;

      const price = q.regularMarketPrice ?? q.regularMarketPreviousClose ?? null;
      const marketCapCr = q.marketCap != null ? q.marketCap / 1e7 : 0;
      const avgVol3m = q.averageDailyVolume3Month ?? 0;
      const avgTradedValCr = price != null && price > 0 ? (avgVol3m * price) / 1e7 : 0;

      if (marketCapCr >= cfg.MIN_MCAP_CR && avgTradedValCr >= cfg.MIN_AVG_TRADED_VALUE_CR) {
        qualifying.push({
          symbol: item.symbol,
          name: item.name || q.longName || q.shortName || item.symbol,
          yahooSymbol: item.yahooSymbol,
          marketCapCr,
          avgTradedValCr,
          price: price != null ? Number(price.toFixed(2)) : null,
          high52: q.fiftyTwoWeekHigh != null ? Number(q.fiftyTwoWeekHigh.toFixed(2)) : null,
          low52: q.fiftyTwoWeekLow != null ? Number(q.fiftyTwoWeekLow.toFixed(2)) : null,
          sector: 'Unclassified',
          sectorUpdatedAt: ''
        });
      }
    }

    if (qualifying.length === 0) {
      throw new Error('Stage 2 yielded 0 stocks meeting MCap & liquidity thresholds');
    }

    // Rank by market cap descending & assign cap tier
    qualifying.sort((a, b) => b.marketCapCr - a.marketCapCr);
    for (let i = 0; i < qualifying.length; i++) {
      qualifying[i].rank = i + 1;
      qualifying[i].cap = classifyCapByRank(i + 1);
      qualifying[i].defaultCap = qualifying[i].cap;
    }

    // Resolve sectors (Nifty 500 map + monthly cached Yahoo assetProfile)
    let targetSubsetIndices = null;
    if (maxStocks && maxStocks < qualifying.length) {
      targetSubsetIndices = new Set(Array.from({ length: maxStocks }, (_, i) => i));
    }

    await resolveSectorsForRows(yf, qualifying, nifty500Map, cachedBySym, cfg, targetSubsetIndices);

    // Save full universe to config/universe.csv (commit-ready rule-based catalog)
    // Even during MAX_STOCKS test runs, persist config/universe.csv ONLY if full sectors were resolved
    // or if existing CSV wasn't yet the rule-based universe (< 100 rows), so we never regress cached sectors.
    if (!maxStocks || cachedRows.length < 100) {
      saveUniverseCsv(qualifying, UNIVERSE_CSV_FILE);
    }

    const finalList = maxStocks ? qualifying.slice(0, maxStocks) : qualifying;
    return {
      universe: finalList,
      fullUniverseCount: qualifying.length,
      stage1Count: stage1.length,
      quoteMap,
      reusedSavedUniverse: false,
      maxStocksApplied: maxStocks
    };
  } catch (err) {
    console.error(`ERROR: Rule-based universe download failed (${err.message}). Reusing last saved config/universe.csv.`);
    if (cachedRows.length === 0) {
      throw new Error(`Universe build failed and no saved config/universe.csv exists: ${err.message}`);
    }
    const finalList = maxStocks ? cachedRows.slice(0, maxStocks) : cachedRows;
    return {
      universe: finalList,
      fullUniverseCount: cachedRows.length,
      stage1Count: null,
      quoteMap: new Map(),
      reusedSavedUniverse: true,
      universeBuildError: err.message,
      maxStocksApplied: maxStocks
    };
  }
}

module.exports = {
  CONFIG_FILE,
  UNIVERSE_CSV_FILE,
  loadScreenerConfig,
  loadStockUniverse,
  saveUniverseCsv,
  classifyCapByRank,
  buildAndSaveUniverse
};
