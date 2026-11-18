#!/usr/bin/env node
/**
 * scripts/build-static-data.js
 * Static data generator for GitHub Pages (no server required).
 * Uses lib/staticSync.js -> lib/marketService.js:
 *   - Routes Yahoo Finance calls through Cloudflare Worker when YAHOO_WORKER_URL / WORKER_URL is set.
 *   - Writes web/data/market.json, web/data/stocks/<SYMBOL>.json, web/data/history/<SYMBOL>.json.
 *
 * Strict Freshness / Loud Failure Policy:
 *   - Never writes fake or invented numbers.
 *   - Fails loudly (exit code 1) if the fetched sessionDate !== expected latest NSE trading session
 *     or if live market fetch failed, preventing silent stale deploys on GitHub Actions.
 */

const { syncStaticData } = require('../lib/staticSync');
const { getLatestCompletedSessionDate } = require('../lib/nseCalendar');

async function main() {
  const startedAt = Date.now();
  const expectedSession = getLatestCompletedSessionDate(new Date());
  const maxStocks = process.env.MAX_STOCKS ? ` (MAX_STOCKS=${process.env.MAX_STOCKS})` : '';
  console.log(`Building rule-based NSE static market dataset${maxStocks} • Expected EOD session: ${expectedSession}...`);

  const res = await syncStaticData({ force: true, allowStaleWrite: false });

  if (!res.ok) {
    if (res.code === 'SESSION_MISMATCH') {
      console.error(
        `ERROR [SESSION MISMATCH]: ${res.reason} ` +
        `(${res.tradingDaysOld ?? '?'} trading day(s) behind). Refusing to overwrite web/data/market.json or silently deploy stale data.`
      );
    } else {
      console.error(`ERROR [STALE/UNAVAILABLE]: Failed to build fresh market data (${res.reason}). Preserving previous web/data/ files and failing workflow loudly.`);
    }
    process.exitCode = 1;
    return;
  }

  const marketData = res.marketData;
  const failedSymbols = Array.isArray(marketData.meta.failedSymbols) ? marketData.meta.failedSymbols : [];
  const skippedSymbols = Array.isArray(marketData.meta.skippedSymbols) ? marketData.meta.skippedSymbols : [];

  if (skippedSymbols.length > 0) {
    console.log(`Skipped ${skippedSymbols.length} stock(s) on history/volume rules (<252 bars or >10% zero-vol).`);
  }
  if (failedSymbols.length > 0) {
    console.error(`WARNING: Failed to fetch ${failedSymbols.length} stock(s) after retries: ${failedSymbols.join(', ')}. Recorded in meta.failedSymbols.`);
  }

  const elapsedSec = ((Date.now() - startedAt) / 1000).toFixed(1);
  console.log(
    `Build complete in ${elapsedSec}s: session=${res.sessionDate}, web/data/market.json (${res.stocksCount}/${marketData.meta.universeTotal} stocks, ${skippedSymbols.length} skipped, ${failedSymbols.length} failed), ` +
    `${res.writtenDetails} web/data/stocks/*.json, ${res.writtenHistories} web/data/history/*.json.`
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
