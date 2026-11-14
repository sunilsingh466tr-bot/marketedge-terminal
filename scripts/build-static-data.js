#!/usr/bin/env node
/**
 * scripts/build-static-data.js
 * Static data generator for GitHub Pages (no server required).
 * Uses the rule-based two-stage universe builder & backend quant engine in lib/marketService.js:
 *   - config/universe.csv (auto-updated & cached monthly for sectors)
 *   - web/data/market.json (compact table fields + meta with universe size, skipped, and failed lists)
 *   - web/data/stocks/<SYMBOL>.json (full quant detail & technicals per stock)
 *   - web/data/history/<SYMBOL>.json (last 300 daily bars per stock + ^CRSLDX -> _CRSLDX.json)
 *
 * Respects MAX_STOCKS environment variable for quick test runs.
 * Failure policy: Never writes fake or invented numbers. Logs errors and preserves previous files intact.
 */

const fs = require('fs');
const path = require('path');
const { getMarketData } = require('../lib/marketService');

const WEB_DATA_DIR = path.join(__dirname, '..', 'web', 'data');
const WEB_STOCKS_DIR = path.join(WEB_DATA_DIR, 'stocks');
const WEB_HISTORY_DIR = path.join(WEB_DATA_DIR, 'history');
const MARKET_OUT_FILE = path.join(WEB_DATA_DIR, 'market.json');

fs.mkdirSync(WEB_STOCKS_DIR, { recursive: true });
fs.mkdirSync(WEB_HISTORY_DIR, { recursive: true });

function sanitizeSymbolFilename(symbol) {
  return String(symbol || '').trim().replace(/[^A-Za-z0-9_-]/g, '_');
}

function writeJsonAtomic(filePath, obj, pretty = false) {
  const tmpPath = `${filePath}.tmp`;
  const text = pretty ? JSON.stringify(obj, null, 2) : JSON.stringify(obj);
  fs.writeFileSync(tmpPath, text, 'utf-8');
  fs.renameSync(tmpPath, filePath);
}

async function main() {
  const startedAt = Date.now();
  const maxStocks = process.env.MAX_STOCKS ? ` (MAX_STOCKS=${process.env.MAX_STOCKS})` : '';
  console.log(`Building rule-based NSE static market dataset${maxStocks}...`);

  const marketData = await getMarketData(true);

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
    console.error(`ERROR: Failed to build fresh market data (${reason}). Keeping previous web/data/ files without writing fake numbers.`);
    if (!fs.existsSync(MARKET_OUT_FILE)) {
      process.exitCode = 1;
    }
    return;
  }

  const failedSymbols = Array.isArray(marketData.meta.failedSymbols) ? marketData.meta.failedSymbols : [];
  const skippedSymbols = Array.isArray(marketData.meta.skippedSymbols) ? marketData.meta.skippedSymbols : [];

  if (skippedSymbols.length > 0) {
    console.log(`Skipped ${skippedSymbols.length} stock(s) on history/volume rules (<252 bars or >10% zero-vol).`);
  }
  if (failedSymbols.length > 0) {
    console.error(`ERROR: Failed to fetch ${failedSymbols.length} stock(s) after retries: ${failedSymbols.join(', ')}. Recorded in meta.failedSymbols; preserving previous files if any.`);
  }

  // 1. Write compact web/data/market.json
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

  // 2. Write web/data/stocks/<SYMBOL>.json (full quant detail per loaded stock)
  const detailsMap = marketData._detailsBySymbol || new Map();
  let writtenDetails = 0;
  for (const [sym, detailObj] of detailsMap.entries()) {
    const outFile = path.join(WEB_STOCKS_DIR, `${sanitizeSymbolFilename(sym)}.json`);
    writeJsonAtomic(outFile, detailObj, false);
    writtenDetails++;
  }

  // 3. Write web/data/history/<SYMBOL>.json (last 300 daily bars per loaded stock + ^CRSLDX)
  const historyMap = marketData._historyBySymbol || new Map();
  let writtenHistories = 0;
  for (const [sym, histObj] of historyMap.entries()) {
    const outFile = path.join(WEB_HISTORY_DIR, `${sanitizeSymbolFilename(sym)}.json`);
    writeJsonAtomic(outFile, histObj, false);
    writtenHistories++;
  }

  const elapsedSec = ((Date.now() - startedAt) / 1000).toFixed(1);
  console.log(
    `Build complete in ${elapsedSec}s: web/data/market.json (${marketData.stocks.length}/${marketData.meta.universeTotal} stocks, ${skippedSymbols.length} skipped, ${failedSymbols.length} failed), ` +
    `${writtenDetails} web/data/stocks/*.json, ${writtenHistories} web/data/history/*.json.`
  );
}

main()
  .then(() => {
    process.exit(process.exitCode || 0);
  })
  .catch(err => {
    console.error('FATAL build-static-data error:', err);
    process.exit(1);
  });
