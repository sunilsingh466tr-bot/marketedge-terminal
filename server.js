/**
 * server.js
 * Express Backend for MarketEdge Terminal.
 * Serves /api/market, /api/history/:symbol, /api/health, and static /web assets on port 3000.
 */

const express = require('express');
const path = require('path');
const { getMarketData, getSymbolHistory } = require('./lib/marketService');
const { getLatestCompletedSessionDate, formatISTDateTime } = require('./lib/nseCalendar');

const envPort = process.env.PORT;
const PORT = Number(process.env.DEFAULT_APP_PORT || (envPort && envPort !== process.env.NGINX_PORT ? envPort : 3000));
const WEB_DIR = path.join(__dirname, 'web');

const app = express();

// CORS middleware (supports Android WebView / local dev)
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
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
  // Warm market data cache asynchronously on startup
  getMarketData(false)
    .then(d => console.log(`Initial market snapshot ready: ${d.stocks.length} stocks, session ${d.meta.sessionDate}`))
    .catch(e => console.warn(`Initial cache warmup failed: ${e.message}`));
});

process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
});
