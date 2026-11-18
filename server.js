/**
 * server.js
 * Express Backend for MarketEdge Terminal.
 * Serves /api/market, /api/history/:symbol, /api/health, and static /web assets on port 3000.
 */

const express = require('express');
const path = require('path');
const { getMarketData, getStockDetail, getSymbolHistory, loadScreenerConfig } = require('./lib/marketService');
const { syncStaticData, readCurrentStaticSessionDate } = require('./lib/staticSync');
const { fetchLiveQuotesCached, resolveWorkerUrl } = require('./lib/yahooClient');
const { getLatestCompletedSessionDate, formatISTDateTime } = require('./lib/nseCalendar');

const envPort = process.env.PORT;
const PORT = Number(process.env.DEFAULT_APP_PORT || (envPort && envPort !== process.env.NGINX_PORT ? envPort : 3000));
const WEB_DIR = path.join(__dirname, 'web');

const app = express();
app.use(express.json({ limit: '1mb' }));

// CORS middleware (supports Android WebView / local dev)
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.get('/api/health', (req, res) => {
  res.json({
    ok: true,
    nowIST: formatISTDateTime(new Date()),
    expectedLatestSession: getLatestCompletedSessionDate(new Date())
  });
});

app.get('/api/worker/health', (req, res) => {
  const cfg = loadScreenerConfig();
  res.json({
    ok: true,
    service: 'marketedge-local-worker-proxy',
    externalWorkerUrl: resolveWorkerUrl(cfg) || null,
    cacheTtlSeconds: cfg.WORKER_CACHE_TTL_SECONDS || 20,
    nowIST: formatISTDateTime(new Date())
  });
});

async function handleLocalWorkerQuotes(req, res) {
  try {
    const rawParam = req.query.symbols ? String(req.query.symbols).split(',') : (req.body?.symbols || []);
    const cfg = loadScreenerConfig();
    const ttl = cfg.WORKER_CACHE_TTL_SECONDS || 20;
    const extWorker = resolveWorkerUrl(cfg);
    const payload = await fetchLiveQuotesCached(rawParam, ttl, extWorker);
    res.setHeader('Cache-Control', `public, max-age=${payload.cacheTtlSeconds || 20}`);
    res.json(payload);
  } catch (err) {
    console.error('/api/worker/quotes error:', err.message);
    res.status(502).json({
      error: 'Live worker quotes unavailable',
      message: err.message
    });
  }
}

app.get('/api/worker/quotes', handleLocalWorkerQuotes);
app.post('/api/worker/quotes', handleLocalWorkerQuotes);

app.get('/api/market', async (req, res) => {
  try {
    const force = req.query.refresh === '1';
    const data = await getMarketData(force);
    res.setHeader('Cache-Control', 'no-cache');
    res.json(data);
  } catch (err) {
    console.error('/api/market error:', err.message);
    res.status(503).json({
      error: 'Market data unavailable',
      message: err.message,
      meta: {
        asOf: null,
        generatedAt: new Date().toISOString(),
        isStale: true,
        source: 'Unavailable'
      }
    });
  }
});

app.get('/api/stock/:symbol', async (req, res) => {
  try {
    const detail = await getStockDetail(req.params.symbol);
    res.setHeader('Cache-Control', 'no-cache');
    res.json(detail);
  } catch (err) {
    console.error(`/api/stock/${req.params.symbol} error:`, err.message);
    res.status(404).json({
      error: 'Stock detail unavailable',
      symbol: req.params.symbol,
      message: err.message
    });
  }
});

app.get('/api/history/:symbol', async (req, res) => {
  try {
    const history = await getSymbolHistory(req.params.symbol);
    res.setHeader('Cache-Control', 'no-cache');
    res.json(history);
  } catch (err) {
    console.error(`/api/history/${req.params.symbol} error:`, err.message);
    res.status(503).json({
      error: 'History unavailable',
      symbol: req.params.symbol,
      message: err.message
    });
  }
});

// Background static web/data sync manager (keeps live Express preview web/data/market.json fresh after 15:30 IST close)
let activeSyncPromise = null;
let lastSyncResult = null;

function ensureStaticDataFresh(force = false) {
  const expected = getLatestCompletedSessionDate(new Date());
  const currentSession = readCurrentStaticSessionDate();
  if (!force && currentSession === expected) {
    return null;
  }
  if (activeSyncPromise) {
    return activeSyncPromise;
  }

  console.log(`Static data sync started: current="${currentSession || 'none'}", expected="${expected}"`);
  activeSyncPromise = syncStaticData({ force: true, allowStaleWrite: false })
    .then(res => {
      lastSyncResult = {
        at: new Date().toISOString(),
        ok: res.ok,
        code: res.code,
        expectedSession: res.expectedSession,
        actualSession: res.actualSession,
        reason: res.reason || null
      };
      if (res.ok) {
        console.log(`Static data sync complete: session=${res.sessionDate}, stocks=${res.stocksCount}`);
      } else {
        console.warn(`Static data sync did not complete (${res.code}): ${res.reason}`);
      }
      return res;
    })
    .catch(err => {
      lastSyncResult = {
        at: new Date().toISOString(),
        ok: false,
        code: 'ERROR',
        expectedSession: expected,
        actualSession: currentSession,
        reason: err.message
      };
      console.error('Static data sync failed:', err.message);
      return lastSyncResult;
    })
    .finally(() => {
      activeSyncPromise = null;
    });

  return activeSyncPromise;
}

app.get('/api/sync-status', (req, res) => {
  const expected = getLatestCompletedSessionDate(new Date());
  const current = readCurrentStaticSessionDate();
  res.json({
    expectedSession: expected,
    currentSession: current,
    isStale: !current || current < expected,
    syncing: Boolean(activeSyncPromise),
    lastSyncResult
  });
});

app.post('/api/sync-static', (req, res) => {
  const expected = getLatestCompletedSessionDate(new Date());
  const current = readCurrentStaticSessionDate();
  if (current === expected && req.query.force !== '1') {
    return res.json({
      expectedSession: expected,
      currentSession: current,
      isStale: false,
      syncing: false,
      message: 'Already up to date'
    });
  }
  ensureStaticDataFresh(req.query.force === '1');
  res.status(202).json({
    expectedSession: expected,
    currentSession: current,
    isStale: true,
    syncing: true,
    message: 'EOD refresh running in background'
  });
});

// Serve static frontend files
app.use(express.static(WEB_DIR, {
  setHeaders: (res) => {
    res.setHeader('Cache-Control', 'no-cache');
  }
}));

// Fallback to web/index.html
app.get('/{*splat}', (req, res) => {
  res.sendFile(path.join(WEB_DIR, 'index.html'));
});

const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`MarketEdge Express Backend listening on http://0.0.0.0:${PORT}`);
  // Sync static web/data/market.json if stale on startup, else warm memory cache
  const expected = getLatestCompletedSessionDate(new Date());
  const current = readCurrentStaticSessionDate();
  if (!current || current < expected) {
    ensureStaticDataFresh(true);
  } else {
    getMarketData(false)
      .then(d => console.log(`Initial market snapshot ready: ${d.stocks.length} stocks, session ${d.meta.sessionDate}`))
      .catch(e => console.warn(`Initial cache warmup failed: ${e.message}`));
  }

  // Check every 15 minutes if market closed & static web/data/market.json needs EOD update
  setInterval(() => {
    ensureStaticDataFresh(false);
  }, 15 * 60 * 1000).unref();
});

process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
});
