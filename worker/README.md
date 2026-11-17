# MarketEdge Yahoo Finance Cloudflare Worker

Lightweight Cloudflare Worker that provides:
1. **Intraday Delayed Live Quotes (`GET /quotes?symbols=...`, `POST /quotes`)** with **20-second** Cloudflare Edge + isolate caching (`QUOTE_CACHE_TTL`, clamped 15–30s) and CORS enabled for GitHub Pages.
2. **EOD Build Proxy Routes (`/yahoo/quotes`, `/yahoo/chart`, `/yahoo/quoteSummary`, `/yahoo/fundamentals`)** so GitHub Actions workflows route Yahoo Finance traffic through Cloudflare IPs instead of shared CI runner IPs.

## Deploy to Cloudflare Workers (Free Tier)

```bash
cd worker
npx wrangler login
npx wrangler deploy
```

After deploying, Wrangler prints your Worker URL, e.g.:
`https://marketedge-yahoo-worker.<your-subdomain>.workers.dev`

## Connect to Site & GitHub Actions Build

1. **For GitHub Actions EOD Build & Frontend Live Polling:**
   - Set `"WORKER_URL": "https://marketedge-yahoo-worker.<your-subdomain>.workers.dev"` in `config/screener-config.json`, **OR**
   - Set Repository Variable `YAHOO_WORKER_URL` in GitHub (`Settings -> Secrets and variables -> Actions -> Variables`).
2. **Test Endpoints:**
   ```bash
   curl "https://marketedge-yahoo-worker.<your-subdomain>.workers.dev/health"
   curl "https://marketedge-yahoo-worker.<your-subdomain>.workers.dev/quotes?symbols=^NSEI,RELIANCE.NS,TCS.NS"
   ```
