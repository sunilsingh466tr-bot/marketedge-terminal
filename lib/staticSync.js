/**
 * lib/staticSync.js
 * Shared writer that builds and atomically persists:
 *   - web/data/market.json
 *   - web/data/stocks/<SYMBOL>.json
 *   - web/data/history/<SYMBOL>.json
 * Used by both scripts/build-static-data.js (CI/CLI) and server.js (auto EOD sync).
 */

const fs = require('fs');
const path = require('path');
const { getMarketData } = require('./marketService');
const { getLatestCompletedSessionDate } = require('./nseCalendar');

const WEB_DATA_DIR = path.join(__dirname, '..', 'web', 'data');
const WEB_STOCKS_DIR = path.join(WEB_DATA_DIR, 'stocks');
const WEB_HISTORY_DIR = path.join(WEB_DATA_DIR, 'history');
const MARKET_OUT_FILE = path.join(WEB_DATA_DIR, 'market.json');

function sanitizeSymbolFilename(symbol) {
  return String(symbol || '').trim().replace(/[^A-Za-z0-9_-]/g, '_');
}

function writeJsonAtomic(filePath, obj, pretty = false) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const tmpPath = `${filePath}.tmp`;
  const text = pretty ? JSON.stringify(obj, null, 2) : JSON.stringify(obj);
  fs.writeFileSync(tmpPath, text, 'utf-8');
  fs.renameSync(tmpPath, filePath);
}

function writeStaticArtifacts(marketData) {
  fs.mkdirSync(WEB_STOCKS_DIR, { recursive: true });
  fs.mkdirSync(WEB_HISTORY_DIR, { recursive: true });

  const { cacheHit, cacheAgeSeconds, ...cleanMeta } = marketData.meta;
  const staticPayload = {
    meta: cleanMeta,
    indices: marketData.indices,
    stocks: marketData.stocks,
    fiiDii: marketData.fiiDii,
    breadth: marketData.breadth,
    bullets: marketData.bullets,
    results: marketData.results,
    earningsRadar: marketData.earningsRadar
  };
  writeJsonAtomic(MARKET_OUT_FILE, staticPayload, false);

  const detailsMap = marketData._detailsBySymbol || new Map();
  let writtenDetails = 0;
  for (const [sym, detailObj] of detailsMap.entries()) {
    const outFile = path.join(WEB_STOCKS_DIR, `${sanitizeSymbolFilename(sym)}.json`);
    writeJsonAtomic(outFile, detailObj, false);
    writtenDetails++;
  }

  const historyMap = marketData._historyBySymbol || new Map();
  let writtenHistories = 0;
  for (const [sym, histObj] of historyMap.entries()) {
    const outFile = path.join(WEB_HISTORY_DIR, `${sanitizeSymbolFilename(sym)}.json`);
    writeJsonAtomic(outFile, histObj, false);
    writtenHistories++;
  }

  return {
    sessionDate: marketData.meta.sessionDate,
    stocksCount: marketData.stocks.length,
    writtenDetails,
    writtenHistories
  };
}

function readCurrentStaticSessionDate() {
  try {
    if (!fs.existsSync(MARKET_OUT_FILE)) return null;
    const raw = JSON.parse(fs.readFileSync(MARKET_OUT_FILE, 'utf-8'));
    return raw?.meta?.sessionDate || null;
  } catch (e) {
    return null;
  }
}

async function syncStaticData({ force = true, allowStaleWrite = false } = {}) {
  const expectedSession = getLatestCompletedSessionDate(new Date());
  const marketData = await getMarketData(force);

  if (
    !marketData ||
    !marketData.meta ||
    marketData.meta.fromSnapshot ||
    !Array.isArray(marketData.stocks) ||
    marketData.stocks.length === 0 ||
    !Array.isArray(marketData.indices) ||
    marketData.indices.length === 0
  ) {
    const reason = marketData?.meta?.liveError || 'Upstream market feed failed or returned fallback snapshot';
    return {
      ok: false,
      code: 'UNAVAILABLE',
      expectedSession,
      actualSession: marketData?.meta?.sessionDate || null,
      reason,
      marketData
    };
  }

  const actualSession = marketData.meta.sessionDate;
  if (!actualSession || (actualSession !== expectedSession && !allowStaleWrite)) {
    return {
      ok: false,
      code: 'SESSION_MISMATCH',
      expectedSession,
      actualSession: actualSession || null,
      tradingDaysOld: marketData.meta.tradingDaysOld ?? null,
      reason: `Fetched EOD sessionDate="${actualSession || 'none'}" does NOT match expected latest NSE trading day "${expectedSession}"`,
      marketData
    };
  }

  const stats = writeStaticArtifacts(marketData);
  return {
    ok: true,
    code: 'OK',
    expectedSession,
    actualSession,
    ...stats,
    marketData
  };
}

module.exports = {
  WEB_DATA_DIR,
  MARKET_OUT_FILE,
  sanitizeSymbolFilename,
  writeJsonAtomic,
  writeStaticArtifacts,
  readCurrentStaticSessionDate,
  syncStaticData
};
