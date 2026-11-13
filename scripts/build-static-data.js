#!/usr/bin/env node
/**
 * scripts/build-static-data.js
 * Static data generator for GitHub Pages (no server required).
 * Reuses the exact data fetching & quant engine from lib/marketService.js
 * and writes:
 *   - web/data/market.json
 *   - web/data/history/<SAFE_SYMBOL>.json (every stock + Nifty 500 ^CRSLDX -> _CRSLDX.json)
 *
 * Failure policy: Never writes fake or invented numbers. If an upstream source fails,
 * keeps any previously generated file intact and logs an explicit error.
 */

const fs = require('fs');
const path = require('path');
const {
  STOCK_UNIVERSE,
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
  let hasErrors = false;
  const startedAt = Date.now();
  console.log('Building static MarketEdge data in web/data/ ...');

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
      hasErrors = true;
      if (!fs.existsSync(MARKET_OUT_FILE)) {
        process.exitCode = 1;
      }
    } else {
      const { cacheHit, cacheAgeSeconds, ...cleanMeta } = marketData.meta;
      const staticPayload = {
        ...marketData,
        meta: cleanMeta
      };
      writeJsonAtomic(MARKET_OUT_FILE, staticPayload);
      console.log(`Wrote web/data/market.json (${marketData.stocks.length} stocks, ${marketData.indices.length} indices, session ${marketData.meta.sessionDate})`);
    }
  } catch (err) {
    console.error(`ERROR: Market data generation failed (${err.message}). Keeping previous web/data/market.json without writing fake numbers.`);
    hasErrors = true;
    if (!fs.existsSync(MARKET_OUT_FILE)) {
      process.exitCode = 1;
    }
  }

  // 2. Write daily OHLCV history for every stock in universe AND Nifty 500 (^CRSLDX -> _CRSLDX.json)
  const historyTargets = [...STOCK_UNIVERSE.map(s => s.symbol), '^CRSLDX'];
  let writtenHistories = 0;

  for (const sym of historyTargets) {
    const safeName = sanitizeSymbolFilename(sym);
    const outFile = path.join(WEB_HISTORY_DIR, `${safeName}.json`);

    try {
      const hist = await getSymbolHistory(sym);
      if (!hist || hist.fromSnapshot || !Array.isArray(hist.bars) || hist.bars.length === 0) {
        const reason = hist?.liveError || 'Upstream OHLCV feed failed or returned fallback snapshot';
        console.error(`ERROR: Failed to fetch live OHLCV for ${sym} (${reason}). Keeping previous ${path.relative(process.cwd(), outFile)}.`);
        hasErrors = true;
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
      hasErrors = true;
    }
  }

  const elapsedMs = Date.now() - startedAt;
  console.log(`Static history files updated: ${writtenHistories}/${historyTargets.length} in ${elapsedMs} ms.`);

  if (hasErrors && !fs.existsSync(MARKET_OUT_FILE)) {
    process.exitCode = 1;
  }
}

main().catch(err => {
  console.error('FATAL build-static-data error:', err);
  process.exit(1);
});
