#!/usr/bin/env node
/**
 * scripts/build-static-data.js
 * Static data generator for GitHub Pages (no server required).
 * Loads STOCK_UNIVERSE from config/universe.csv via lib/marketService.js,
 * computes quant/RS metrics, and writes:
 *   - web/data/market.json (including meta.failedSymbols for skipped stocks)
 *   - web/data/history/<SAFE_SYMBOL>.json (every loaded stock + Nifty 500 ^CRSLDX -> _CRSLDX.json)
 *
 * Failure policy: Never writes fake or invented numbers. If a data source fails,
 * logs an explicit error and preserves any previously generated file intact.
 */

const fs = require('fs');
const path = require('path');
const {
  loadStockUniverse,
  getMarketData,
  getSymbolHistory
} = require('../lib/marketService');
const { formatISTDateTime } = require('../lib/nseCalendar');

const WEB_DATA_DIR = path.join(__dirname, '..', 'web', 'data');
const WEB_HISTORY_DIR = path.join(WEB_DATA_DIR, 'history');
const MARKET_OUT_FILE = path.join(WEB_DATA_DIR, 'market.json');

fs.mkdirSync(WEB_HISTORY_DIR, { recursive: true });

function sanitizeSymbolFilename(symbol) {
  return String(symbol || '').trim().replace(/[^A-Za-z0-9_-]/g, '_');
}

function writeJsonAtomic(filePath, obj) {
  const tmpPath = `${filePath}.tmp`;
  fs.writeFileSync(tmpPath, JSON.stringify(obj, null, 2), 'utf-8');
  fs.renameSync(tmpPath, filePath);
}

async function main() {
  let hasFatalErrors = false;
  const startedAt = Date.now();
  const universe = loadStockUniverse();
  console.log(`Loaded ${universe.length} stocks from config/universe.csv. Building static data in web/data/ ...`);

  let failedSet = new Set();

  // 1. Fetch & compute full market dataset + quant + RS percentiles via marketService
  try {
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
      console.error(`ERROR: Failed to fetch fresh market data (${reason}). Keeping previous web/data/market.json without writing fake numbers.`);
      hasFatalErrors = true;
      if (!fs.existsSync(MARKET_OUT_FILE)) {
        process.exitCode = 1;
      }
    } else {
      const failedSymbols = Array.isArray(marketData.meta.failedSymbols) ? marketData.meta.failedSymbols : [];
      failedSet = new Set(failedSymbols);

      if (failedSymbols.length > 0) {
        console.error(`ERROR: Failed to load ${failedSymbols.length} stock(s): ${failedSymbols.join(', ')}. Recorded in meta.failedSymbols and skipped without fake numbers.`);
      }

      const { cacheHit, cacheAgeSeconds, ...cleanMeta } = marketData.meta;
      const staticPayload = {
        ...marketData,
        meta: {
          ...cleanMeta,
          universeTotal: cleanMeta.universeTotal ?? universe.length,
          universeCount: marketData.stocks.length,
          failedSymbols
        }
      };
      writeJsonAtomic(MARKET_OUT_FILE, staticPayload);
      console.log(`Wrote web/data/market.json (${marketData.stocks.length} of ${staticPayload.meta.universeTotal} stocks, ${marketData.indices.length} indices, session ${marketData.meta.sessionDate})`);
    }
  } catch (err) {
    console.error(`ERROR: Market data generation failed (${err.message}). Keeping previous web/data/market.json without writing fake numbers.`);
    hasFatalErrors = true;
    if (!fs.existsSync(MARKET_OUT_FILE)) {
      process.exitCode = 1;
    }
  }

  // 2. Write daily OHLCV history for every loaded stock AND Nifty 500 (^CRSLDX -> _CRSLDX.json)
  const historyTargets = [...universe.map(s => s.symbol), '^CRSLDX'];
  let writtenHistories = 0;

  for (const sym of historyTargets) {
    const safeName = sanitizeSymbolFilename(sym);
    const outFile = path.join(WEB_HISTORY_DIR, `${safeName}.json`);

    if (failedSet.has(sym)) {
      console.error(`ERROR: Skipping history update for failed symbol ${sym}; keeping previous ${path.relative(process.cwd(), outFile)} if present.`);
      continue;
    }

    try {
      const hist = await getSymbolHistory(sym);
      if (!hist || hist.fromSnapshot || !Array.isArray(hist.bars) || hist.bars.length === 0) {
        const reason = hist?.liveError || 'Upstream OHLCV feed failed or returned fallback snapshot';
        console.error(`ERROR: Failed to fetch live OHLCV for ${sym} (${reason}). Keeping previous ${path.relative(process.cwd(), outFile)}.`);
        continue;
      }

      const { cacheHit, ...cleanHist } = hist;
      if (!cleanHist.asOfDisplay && cleanHist.asOf) {
        cleanHist.asOfDisplay = formatISTDateTime(cleanHist.asOf);
      }

      writeJsonAtomic(outFile, cleanHist);
      writtenHistories++;
    } catch (err) {
      console.error(`ERROR: History fetch failed for ${sym} (${err.message}). Keeping previous ${path.relative(process.cwd(), outFile)}.`);
    }
  }

  const elapsedMs = Date.now() - startedAt;
  console.log(`Static history files updated: ${writtenHistories}/${historyTargets.length} in ${elapsedMs} ms.`);

  if (hasFatalErrors && !fs.existsSync(MARKET_OUT_FILE)) {
    process.exitCode = 1;
  }
}

main().catch(err => {
  console.error('FATAL build-static-data error:', err);
  process.exit(1);
});
