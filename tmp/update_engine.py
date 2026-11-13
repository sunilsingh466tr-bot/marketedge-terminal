import re

new_engine_code = r'''    // -------------------------------------------------------------
    // DETERMINISTIC PRNG AND DATE-ALIGNED 252-DAY PRICE ENGINE
    // -------------------------------------------------------------
    function mulberry32(seed) {
      let s = seed >>> 0;
      return function() {
        s = (s + 0x6D2B79F5) | 0;
        let t = Math.imul(s ^ (s >>> 15), 1 | s);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
      };
    }

    function hashString(str) {
      let h = 2166136261 >>> 0;
      for (let i = 0; i < str.length; i++) {
        h = Math.imul(h ^ str.charCodeAt(i), 16777619);
      }
      return h >>> 0;
    }

    // Benchmark series initialized deterministically (253 trading sessions ending 12-Nov-2026)
    const BENCHMARK_DATA = {};
    (function initBenchmarkSeries() {
      // 1. Nifty 50: ends at official close 25299.35
      const n50Rng = mulberry32(505050);
      const n50 = new Array(253);
      n50[252] = 25299.35;
      for (let i = 251; i >= 0; i--) {
        const ret = 0.00045 + (n50Rng() - 0.5) * 0.016;
        n50[i] = n50[i + 1] / Math.exp(ret);
      }
      BENCHMARK_DATA["Nifty 50"] = n50;

      // 2. Nifty Midcap 100: ends at official close 58410.20
      const midRng = mulberry32(100100);
      const mid = new Array(253);
      mid[252] = 58410.20;
      for (let i = 251; i >= 0; i--) {
        const ret = 0.00075 + (midRng() - 0.5) * 0.021;
        mid[i] = mid[i + 1] / Math.exp(ret);
      }
      BENCHMARK_DATA["Nifty Midcap 100"] = mid;

      // 3. Nifty 500: ends at official close 23779.30
      const n500Rng = mulberry32(500500);
      const n500 = new Array(253);
      n500[252] = 23779.30;
      for (let i = 251; i >= 0; i--) {
        const ret = 0.00055 + (n500Rng() - 0.5) * 0.017;
        n500[i] = n500[i + 1] / Math.exp(ret);
      }
      BENCHMARK_DATA["Nifty 500"] = n500;
    })();

    // -------------------------------------------------------------
    // QUANTITATIVE RISK ENGINE (252-Day Log-Return Formulation)
    // -------------------------------------------------------------
    const logRet = a => a.slice(1).map((p, i) => Math.log(p / a[i]));
    const mean = a => a.reduce((s, x) => s + x, 0) / a.length;
    const cov = (x, y) => {
      const mx = mean(x), my = mean(y);
      return x.reduce((s, v, i) => s + (v - mx) * (y[i] - my), 0) / (x.length - 1);
    };
    const sd = x => Math.sqrt(cov(x, x));

    function riskStats(stockPx, idxPx, days) { // days: 252 | 126 | 21
      const s = logRet(stockPx.slice(-(days + 1)));
      const i = logRet(idxPx.slice(-(days + 1)));
      const c = cov(s, i);
      const sSD = sd(s);
      const iSD = sd(i);
      return {
        sdStock: sSD * Math.sqrt(252) * 100,
        sdIdx: iSD * Math.sqrt(252) * 100,
        covAnn: c * 252,
        corr: (sSD > 0 && iSD > 0) ? (c / (sSD * iSD)) : 0,
        beta: (iSD > 0) ? (c / (iSD ** 2)) : 1.0
      };
    }

    // Deterministic stock price array (length 253, ending at stock.price)
    function getStockPriceSeries(stock) {
      const rng = mulberry32(hashString(stock.symbol));
      const bmName = stock.benchmarkName || "Nifty 50";
      const bmSeries = BENCHMARK_DATA[bmName] || BENCHMARK_DATA["Nifty 50"];
      const count = bmSeries.length; // 253
      const sPx = new Array(count);
      sPx[count - 1] = stock.price;

      const targetVol = (stock.quant && stock.quant.sd) ? stock.quant.sd : 25.0;
      const targetBeta = (stock.quant && stock.quant.beta) ? stock.quant.beta : 1.25;

      for (let i = count - 2; i >= 0; i--) {
        const idxRet = Math.log(bmSeries[i + 1] / bmSeries[i]);
        const noise = (rng() - 0.5) * 2 * (targetVol / 100 / Math.sqrt(252)) * 0.72;
        const stockRet = targetBeta * idxRet + noise;
        sPx[i] = Math.round((sPx[i + 1] / Math.exp(stockRet)) * 20) / 20;
      }
      return sPx;
    }

    // Construct 252 daily candles deterministically
    function generateCandles(stock) {
      const closes = getStockPriceSeries(stock);
      const n500Closes = BENCHMARK_DATA["Nifty 500"];
      const n = closes.length; // 253
      const rng = mulberry32(hashString(stock.symbol + "_candles"));

      // Moving Averages
      const k21 = 2 / 22;
      let prevEma = closes[0];
      const ema21 = [];
      const sma50 = [];
      const sma200 = [];

      for (let i = 0; i < n; i++) {
        prevEma = (closes[i] * k21) + (prevEma * (1 - k21));
        ema21.push(prevEma);

        if (i >= 49) {
          const sum50 = closes.slice(i - 49, i + 1).reduce((a, b) => a + b, 0);
          sma50.push(sum50 / 50);
        } else {
          sma50.push(closes[i]);
        }

        if (i >= 199) {
          const sum200 = closes.slice(i - 199, i + 1).reduce((a, b) => a + b, 0);
          sma200.push(sum200 / 200);
        } else {
          sma200.push(closes[i]);
        }
      }

      // RSI (14)
      const rsi = [];
      let avgGain = 0, avgLoss = 0;
      for (let i = 0; i < n; i++) {
        if (i === 0) { rsi.push(50); continue; }
        const diff = closes[i] - closes[i - 1];
        const gain = diff > 0 ? diff : 0;
        const loss = diff < 0 ? -diff : 0;
        if (i <= 14) {
          avgGain += gain; avgLoss += loss;
          if (i === 14) { avgGain /= 14; avgLoss /= 14; }
          const rsVal = avgLoss === 0 ? 100 : avgGain / avgLoss;
          rsi.push(100 - (100 / (1 + rsVal)));
        } else {
          avgGain = (avgGain * 13 + gain) / 14;
          avgLoss = (avgLoss * 13 + loss) / 14;
          const rsVal = avgLoss === 0 ? 100 : avgGain / avgLoss;
          rsi.push(100 - (100 / (1 + rsVal)));
        }
      }

      // MACD (12, 26, 9)
      const k12 = 2 / 13;
      const k26 = 2 / 27;
      const k9 = 2 / 10;
      let e12 = closes[0], e26 = closes[0];
      const macd = [];
      for (let i = 0; i < n; i++) {
        e12 = (closes[i] * k12) + (e12 * (1 - k12));
        e26 = (closes[i] * k26) + (e26 * (1 - k26));
        macd.push(e12 - e26);
      }
      const macdSignal = [];
      const macdHist = [];
      let sig = macd[0];
      for (let i = 0; i < n; i++) {
        sig = (macd[i] * k9) + (sig * (1 - k9));
        macdSignal.push(sig);
        macdHist.push(macd[i] - sig);
      }

      // Mansfield RS vs Nifty 500
      const rsNorm = [];
      const baseRatio = closes[0] / n500Closes[0];
      for (let i = 0; i < n; i++) {
        const curRatio = closes[i] / n500Closes[i];
        rsNorm.push((curRatio / baseRatio) * 100);
      }

      // Assemble 252 day candles
      const list = [];
      for (let i = 1; i < n; i++) {
        const close = closes[i];
        const open = closes[i - 1];
        const wickUp = Math.abs(rng()) * (close * 0.012);
        const wickDown = Math.abs(rng()) * (close * 0.012);
        const high = Math.max(open, close) + wickUp;
        const low = Math.min(open, close) - wickDown;
        const vol = Math.floor(450000 + rng() * 1950000);

        list.push({
          date: `D-${n - 1 - i}`,
          open, high, low, close, volume: vol,
          ema21: ema21[i],
          sma50: sma50[i],
          sma200: sma200[i],
          rsi: rsi[i],
          macd: macd[i],
          macdSignal: macdSignal[i],
          macdHist: macdHist[i],
          rs: rsNorm[i]
        });
      }
      return list;
    }

    // CANDLESTICK CHART ENGINE WITH RSI & MACD INDICATORS
    let selectedIndicator = 'RSI';
    let currentModalBenchmark = 'relevant'; // 'relevant' or 'n500'

    function setIndicator(ind, el) {
      selectedIndicator = ind;
      document.querySelectorAll(".ind-btn").forEach(b => b.classList.remove("active"));
      if (el) el.classList.add("active");
      drawStockChart();
    }

    function setTimeframe(tf, el) {
      selectedTimeframe = tf;
      document.querySelectorAll(".tf-btn").forEach(b => b.classList.remove("active"));
      if (el) el.classList.add("active");
      drawStockChart();
    }

    function drawStockChart(inspectedIndex = null) {
      const canvas = document.getElementById("candlestick-canvas");
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      const dpr = window.devicePixelRatio || 1;
      
      const width = canvas.parentElement.clientWidth - 24;
      const isMulti = selectedIndicator === 'ALL';
      const height = isMulti ? 440 : 330;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = width + "px";
      canvas.style.height = height + "px";
      ctx.scale(dpr, dpr);

      let candles = activeStockCandles;
      if (selectedTimeframe === '1M') candles = candles.slice(-21);
      else if (selectedTimeframe === '3M') candles = candles.slice(-63);
      else if (selectedTimeframe === '6M') candles = candles.slice(-126);
      else if (selectedTimeframe === '1Y') candles = candles.slice(-252);

      const n = candles.length;
      if (n === 0) return;

      const isDark = document.body.getAttribute("data-theme") === "dark";
      const greenCol = isDark ? "#00E676" : "#059669";
      const redCol = isDark ? "#FF3B30" : "#DC2626";
      const cyanCol = isDark ? "#00E5FF" : "#0284C7";
      const amberCol = isDark ? "#FFB300" : "#D97706";
      const purpleCol = isDark ? "#A855F7" : "#7C3AED";
      const gridCol = isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)";
      const textCol = isDark ? "#94A3B8" : "#64748B";

      let minP = Infinity, maxP = -Infinity;
      let maxVol = 0;
      candles.forEach(c => {
        minP = Math.min(minP, c.low, c.sma200);
        maxP = Math.max(maxP, c.high);
        maxVol = Math.max(maxVol, c.volume);
      });

      const pRange = (maxP - minP) * 1.08;
      const pZero = maxP + (pRange * 0.04);

      let priceH, volTop, volH, indTop, indH;
      if (isMulti) {
        priceH = height * 0.42;
        volTop = priceH + 6;
        volH = height * 0.10;
        indTop = volTop + volH + 6;
        indH = height - indTop - 6;
      } else {
        priceH = height * 0.54;
        volTop = priceH + 6;
        volH = height * 0.12;
        indTop = volTop + volH + 8;
        indH = height - indTop - 6;
      }

      function getY(val) {
        return ((pZero - val) / pRange) * priceH;
      }
      function getVolY(vol) {
        return volTop + volH - (vol / (maxVol || 1)) * volH;
      }

      // Grid
      ctx.strokeStyle = gridCol;
      ctx.lineWidth = 1;
      for (let i = 0; i <= 3; i++) {
        const gy = (priceH / 3) * i;
        ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(width, gy); ctx.stroke();
      }

      const slotW = width / n;
      const bodyW = Math.max(3, slotW * 0.65);

      // 1. Candlesticks & Volume
      candles.forEach((c, idx) => {
        const cx = idx * slotW + slotW / 2;
        const isUp = c.close >= c.open;
        const color = isUp ? greenCol : redCol;

        const vy = getVolY(c.volume);
        ctx.fillStyle = color;
        ctx.globalAlpha = 0.35;
        ctx.fillRect(cx - bodyW / 2, vy, bodyW, (volTop + volH) - vy);
        ctx.globalAlpha = 1.0;

        const hy = getY(c.high);
        const ly = getY(c.low);
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.2;
        ctx.beginPath(); ctx.moveTo(cx, hy); ctx.lineTo(cx, ly); ctx.stroke();

        const oy = getY(c.open);
        const cy = getY(c.close);
        const topY = Math.min(oy, cy);
        const bH = Math.max(2, Math.abs(cy - oy));
        ctx.fillStyle = color;
        ctx.fillRect(cx - bodyW / 2, topY, bodyW, bH);
      });

      // 2. Moving Average Overlay Lines
      function drawLine(key, color, widthLine) {
        ctx.strokeStyle = color;
        ctx.lineWidth = widthLine;
        ctx.beginPath();
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const cy = getY(c[key]);
          if (idx === 0) ctx.moveTo(cx, cy);
          else ctx.lineTo(cx, cy);
        });
        ctx.stroke();
      }

      drawLine('ema21', cyanCol, 1.8);
      drawLine('sma50', amberCol, 1.6);
      drawLine('sma200', purpleCol, 1.4);

      ctx.fillStyle = textCol;
      ctx.font = "9px 'JetBrains Mono', monospace";
      ctx.fillText("VOL", 6, volTop + 10);

      // 3. Sub-Pane Indicators
      function drawRsPane(topY, paneH) {
        let minRs = Infinity, maxRs = -Infinity;
        candles.forEach(c => { minRs = Math.min(minRs, c.rs); maxRs = Math.max(maxRs, c.rs); });
        const rRange = (maxRs - minRs) || 1;

        ctx.strokeStyle = gridCol;
        ctx.beginPath(); ctx.moveTo(0, topY + paneH/2); ctx.lineTo(width, topY + paneH/2); ctx.stroke();

        ctx.strokeStyle = cyanCol;
        ctx.lineWidth = 1.8;
        ctx.beginPath();
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const ry = topY + paneH - ((c.rs - minRs) / rRange) * (paneH * 0.8) - (paneH * 0.1);
          if (idx === 0) ctx.moveTo(cx, ry); else ctx.lineTo(cx, ry);
        });
        ctx.stroke();

        ctx.fillStyle = cyanCol;
        ctx.font = "bold 9px 'JetBrains Mono', monospace";
        ctx.fillText("RS vs NIFTY 500 (Mansfield)", 6, topY + 11);
      }

      function drawRsiPane(topY, paneH) {
        const y70 = topY + paneH * 0.30;
        const y50 = topY + paneH * 0.50;
        const y30 = topY + paneH * 0.70;

        ctx.fillStyle = isDark ? "rgba(168, 85, 247, 0.05)" : "rgba(124, 58, 237, 0.04)";
        ctx.fillRect(0, y70, width, y30 - y70);

        ctx.strokeStyle = amberCol;
        ctx.setLineDash([3, 3]);
        ctx.beginPath(); ctx.moveTo(0, y70); ctx.lineTo(width, y70); ctx.stroke();

        ctx.strokeStyle = gridCol;
        ctx.beginPath(); ctx.moveTo(0, y50); ctx.lineTo(width, y50); ctx.stroke();

        ctx.strokeStyle = greenCol;
        ctx.beginPath(); ctx.moveTo(0, y30); ctx.lineTo(width, y30); ctx.stroke();
        ctx.setLineDash([]);

        ctx.strokeStyle = purpleCol;
        ctx.lineWidth = 2.0;
        ctx.beginPath();
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const clamped = Math.max(0, Math.min(100, c.rsi || 50));
          const ry = topY + paneH - (clamped / 100 * paneH);
          if (idx === 0) ctx.moveTo(cx, ry); else ctx.lineTo(cx, ry);
        });
        ctx.stroke();

        ctx.fillStyle = purpleCol;
        ctx.font = "bold 9px 'JetBrains Mono', monospace";
        const lastRsi = candles[candles.length - 1].rsi || 50;
        const zone = lastRsi >= 70 ? "OVERBOUGHT" : lastRsi <= 30 ? "OVERSOLD" : lastRsi >= 60 ? "BULLISH ZONE" : "NEUTRAL";
        ctx.fillText(`RSI (14): ${lastRsi.toFixed(1)} • ${zone}`, 6, topY + 11);

        ctx.fillStyle = amberCol;
        ctx.fillText("70 OB", width - 42, y70 - 2);
        ctx.fillStyle = greenCol;
        ctx.fillText("30 OS", width - 42, y30 + 10);
      }

      function drawMacdPane(topY, paneH) {
        let maxAmp = 0;
        candles.forEach(c => {
          maxAmp = Math.max(maxAmp, Math.abs(c.macd || 0), Math.abs(c.macdSignal || 0), Math.abs(c.macdHist || 0));
        });
        maxAmp = maxAmp || 1;

        const zeroY = topY + (paneH / 2);
        ctx.strokeStyle = gridCol;
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(0, zeroY); ctx.lineTo(width, zeroY); ctx.stroke();

        function macdY(v) {
          const half = paneH / 2;
          return zeroY - (v / maxAmp) * (half * 0.85);
        }

        const barW = Math.max(2, slotW * 0.45);
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const hy = macdY(c.macdHist || 0);
          const isPos = (c.macdHist || 0) >= 0;
          ctx.fillStyle = isPos ? greenCol : redCol;
          ctx.globalAlpha = 0.65;
          const top = isPos ? hy : zeroY;
          const bh = Math.max(1.5, Math.abs(hy - zeroY));
          ctx.fillRect(cx - barW / 2, top, barW, bh);
        });
        ctx.globalAlpha = 1.0;

        ctx.strokeStyle = cyanCol;
        ctx.lineWidth = 1.8;
        ctx.beginPath();
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const my = macdY(c.macd || 0);
          if (idx === 0) ctx.moveTo(cx, my); else ctx.lineTo(cx, my);
        });
        ctx.stroke();

        ctx.strokeStyle = amberCol;
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        candles.forEach((c, idx) => {
          const cx = idx * slotW + slotW / 2;
          const sy = macdY(c.macdSignal || 0);
          if (idx === 0) ctx.moveTo(cx, sy); else ctx.lineTo(cx, sy);
        });
        ctx.stroke();

        ctx.fillStyle = cyanCol;
        ctx.font = "bold 9px 'JetBrains Mono', monospace";
        const lastHist = candles[candles.length - 1].macdHist || 0;
        const macdTag = lastHist >= 0 ? "BULLISH CROSSOVER" : "BEARISH MOMENTUM";
        ctx.fillText(`MACD (12, 26, 9) • ${macdTag}`, 6, topY + 11);

        ctx.fillStyle = amberCol;
        ctx.fillText("Sig (Amber)", width - 72, topY + 11);
      }

      if (selectedIndicator === 'RS') {
        drawRsPane(indTop, indH);
      } else if (selectedIndicator === 'RSI') {
        drawRsiPane(indTop, indH);
      } else if (selectedIndicator === 'MACD') {
        drawMacdPane(indTop, indH);
      } else if (isMulti) {
        const subH = (indH - 12) / 3;
        drawRsPane(indTop, subH);
        drawRsiPane(indTop + subH + 6, subH);
        drawMacdPane(indTop + (subH + 6) * 2, subH);
      }

      // Crosshair inspection
      if (inspectedIndex !== null && inspectedIndex >= 0 && inspectedIndex < n) {
        const ic = candles[inspectedIndex];
        const ix = inspectedIndex * slotW + slotW / 2;
        
        ctx.strokeStyle = "rgba(0, 229, 255, 0.55)";
        ctx.setLineDash([3, 3]);
        ctx.beginPath(); ctx.moveTo(ix, 0); ctx.lineTo(ix, height); ctx.stroke();
        ctx.setLineDash([]);

        const legend = document.getElementById("chart-inspection-info");
        if (legend) {
          const isBull = ic.close >= ic.open;
          const rsiTag = ic.rsi >= 70 ? 'OB' : ic.rsi <= 30 ? 'OS' : 'Neutral';
          const macdTag = (ic.macdHist || 0) >= 0 ? '+' : '';
          legend.innerHTML = `
            <span>O: ₹${ic.open.toFixed(1)}</span>
            <span>H: ₹${ic.high.toFixed(1)}</span>
            <span>L: ₹${ic.low.toFixed(1)}</span>
            <span class="${isBull ? 'up' : 'down'}">C: ₹${ic.close.toFixed(1)}</span>
            <span style="color: var(--cyan-rs)">RS: ${(ic.rs || 0).toFixed(0)}</span>
            <span style="color: var(--purple); font-weight:700;">RSI: ${(ic.rsi || 50).toFixed(1)} (${rsiTag})</span>
            <span style="color: ${(ic.macdHist || 0) >= 0 ? 'var(--green)' : 'var(--red)'}; font-weight:700;">MACD: ${(ic.macd || 0).toFixed(1)} | Hist: ${macdTag}${(ic.macdHist || 0).toFixed(1)}</span>
          `;
        }
      }
    }

    // -------------------------------------------------------------
    // QUANTITATIVE RISK MATRIX & COMPARISON RENDERER
    // -------------------------------------------------------------
    function renderQuantSection(stock, mode = 'relevant') {
      currentModalBenchmark = mode;
      const stockPx = getStockPriceSeries(stock);
      const relevantBmName = stock.benchmarkName || (stock.cap === 'Large-Cap' ? 'Nifty 50' : 'Nifty Midcap 100');
      const activeBmName = mode === 'relevant' ? relevantBmName : 'Nifty 500';
      const bmPx = BENCHMARK_DATA[activeBmName] || BENCHMARK_DATA['Nifty 50'];

      const stat252 = riskStats(stockPx, bmPx, 252);
      const stat126 = riskStats(stockPx, bmPx, 126);
      const stat21 = riskStats(stockPx, bmPx, 21);

      // Window returns
      const sRet252 = ((stockPx[252] - stockPx[0]) / stockPx[0]) * 100;
      const sRet126 = ((stockPx[252] - stockPx[126]) / stockPx[126]) * 100;
      const sRet21 = ((stockPx[252] - stockPx[231]) / stockPx[231]) * 100;

      const bmRet252 = ((bmPx[252] - bmPx[0]) / bmPx[0]) * 100;
      const bmRet126 = ((bmPx[252] - bmPx[126]) / bmPx[126]) * 100;
      const bmRet21 = ((bmPx[252] - bmPx[231]) / bmPx[231]) * 100;

      // Alpha vs benchmark
      const alpha252 = sRet252 - (stat252.beta * bmRet252);
      const alpha126 = sRet126 - (stat126.beta * bmRet126);
      const alpha21 = sRet21 - (stat21.beta * bmRet21);

      // 1. Update side-by-side SD values
      const sd1yEl = document.getElementById("sd-1y-val");
      const sd6mEl = document.getElementById("sd-6m-val");
      const sd1mEl = document.getElementById("sd-1m-val");
      if (sd1yEl) sd1yEl.innerText = `${stat252.sdStock.toFixed(1)}%`;
      if (sd6mEl) sd6mEl.innerText = `${stat126.sdStock.toFixed(1)}%`;
      if (sd1mEl) sd1mEl.innerText = `${stat21.sdStock.toFixed(1)}%`;

      // 2. Update KPI summary cards
      const kpiBetaEl = document.getElementById("kpi-quant-beta");
      const kpiCorrEl = document.getElementById("kpi-quant-corr");
      const kpiCovEl = document.getElementById("kpi-quant-cov");
      const kpiBmLabel = document.getElementById("kpi-bm-label");
      if (kpiBetaEl) kpiBetaEl.innerText = stat252.beta.toFixed(2);
      if (kpiCorrEl) kpiCorrEl.innerText = stat252.corr.toFixed(2);
      if (kpiCovEl) kpiCovEl.innerText = (stat252.covAnn * 100).toFixed(2);
      if (kpiBmLabel) kpiBmLabel.innerText = `vs ${activeBmName}`;

      // 3. Update comparison table
      const tbody = document.getElementById("quant-comparison-table-body");
      if (tbody) {
        tbody.innerHTML = `
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; font-weight: 700; color: var(--text-primary);">${stock.symbol} Return</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet252 >= 0 ? 'up' : 'down'}">${sRet252 >= 0 ? '+' : ''}${sRet252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet126 >= 0 ? 'up' : 'down'}">${sRet126 >= 0 ? '+' : ''}${sRet126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet21 >= 0 ? 'up' : 'down'}">${sRet21 >= 0 ? '+' : ''}${sRet21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${activeBmName} Return</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet252 >= 0 ? 'up' : 'down'}">${bmRet252 >= 0 ? '+' : ''}${bmRet252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet126 >= 0 ? 'up' : 'down'}">${bmRet126 >= 0 ? '+' : ''}${bmRet126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet21 >= 0 ? 'up' : 'down'}">${bmRet21 >= 0 ? '+' : ''}${bmRet21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; font-weight: 700; color: var(--cyan-rs);">Alpha (Jensen's vs ${activeBmName})</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha252 >= 0 ? 'up' : 'down'}">${alpha252 >= 0 ? '+' : ''}${alpha252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha126 >= 0 ? 'up' : 'down'}">${alpha126 >= 0 ? '+' : ''}${alpha126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha21 >= 0 ? 'up' : 'down'}">${alpha21 >= 0 ? '+' : ''}${alpha21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${stock.symbol} SD (Annualized)</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat252.sdStock.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat126.sdStock.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700; color: var(--amber);">${stat21.sdStock.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${activeBmName} SD (Annualized)</td>
            <td style="padding: 6px 8px; text-align: right;">${stat252.sdIdx.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;">${stat126.sdIdx.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;">${stat21.sdIdx.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">Annualized Covariance (Cov × 252)</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat252.covAnn * 100).toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat126.covAnn * 100).toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat21.covAnn * 100).toFixed(2)}</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">Correlation (ρ vs ${activeBmName})</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat252.corr.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat126.corr.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat21.corr.toFixed(2)}</td>
          </tr>
          <tr>
            <td style="padding: 6px 8px; color: var(--text-secondary);">Beta (β vs ${activeBmName})</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 800; color: var(--cyan-rs);">${stat252.beta.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 800; color: var(--cyan-rs);">${stat126.beta.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 800; color: var(--cyan-rs);">${stat21.beta.toFixed(2)}</td>
          </tr>
        `;
      }
    }

    function switchBenchmarkInModal(mode) {
      const stock = STOCKS.find(s => s.symbol === activeStockSymbol);
      if (!stock) return;
      document.getElementById("btn-bm-relevant")?.classList.toggle("active", mode === 'relevant');
      document.getElementById("btn-bm-n500")?.classList.toggle("active", mode === 'n500');
      renderQuantSection(stock, mode);
    }

    // OPEN STOCK DOSSIER MODAL
    function openModal(symbol) {
      const stock = STOCKS.find(s => s.symbol === symbol);
      if (!stock) return;

      activeStockSymbol = symbol;
      activeStockCandles = generateCandles(stock);

      const body = document.getElementById("modal-body");
      const alpha = (stock.threeM - stock.benchmarkReturn).toFixed(1);
      const dist52 = (((stock.price - stock.high52) / stock.high52) * 100).toFixed(1);
      const rangePercent = Math.min(100, Math.max(0, ((stock.price - stock.low52) / (stock.high52 - stock.low52)) * 100));
      const relevantBmName = stock.benchmarkName || (stock.cap === 'Large-Cap' ? 'Nifty 50' : 'Nifty Midcap 100');

      body.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px;">
          <div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <h2 style="font-family: var(--font-mono); font-size: 26px; font-weight: 800; color: var(--text-primary);">${stock.symbol}</h2>
              <span class="rs-score-badge ${stock.rs >= 95 ? 'rs-super' : 'rs-high'}">RS ${stock.rs}</span>
              <span class="cap-badge">${stock.cap}</span>
              <span style="font-size: 10px; background: var(--card); border: 1px solid var(--border); padding: 2px 6px; border-radius: 4px; color: var(--text-secondary);">Benchmark: ${relevantBmName}</span>
            </div>
            <div style="font-size: 13px; color: var(--text-secondary); margin-top: 2px;">${stock.name} • ${stock.sector}</div>
          </div>
          <div style="text-align: right;">
            <div id="modal-live-price" style="font-size: 24px; font-weight: 800; font-family: var(--font-mono);">₹${stock.price.toLocaleString()}</div>
            <div class="up" style="font-weight: 700; font-size: 13px;">${stock.chg >= 0 ? '+' : ''}${stock.chg}% Today • ${dist52}% from 52W High</div>
          </div>
        </div>

        <!-- 52-WEEK RANGE BAR -->
        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: var(--font-mono); color: var(--text-secondary);">
            <span>52W Low: ₹${stock.low52.toLocaleString()}</span>
            <span style="color: var(--text-primary); font-weight: 700;">Current: ₹${stock.price.toLocaleString()} (${rangePercent.toFixed(0)}% of range)</span>
            <span>52W High: ₹${stock.high52.toLocaleString()}</span>
          </div>
          <div class="range52-bar">
            <div class="range52-fill" style="width: ${rangePercent}%;"></div>
            <div class="range52-thumb" style="left: ${rangePercent}%;"></div>
          </div>
        </div>

        <!-- INTERACTIVE CANDLESTICK CHART WITH MOVING AVERAGES & MOMENTUM INDICATORS -->
        <div class="chart-box">
          <div class="chart-legend" style="margin-bottom: 6px;">
            <div id="chart-inspection-info" style="display: flex; gap: 10px; font-size: 11px; flex-wrap: wrap;">
              <span style="color: var(--cyan-rs)">21 EMA (Cyan)</span>
              <span style="color: var(--amber)">50 SMA (Amber)</span>
              <span style="color: var(--purple)">200 SMA (Purple)</span>
              <span style="font-size: 10px; color: var(--text-tertiary); margin-left: 6px;">● Illustrative EOD Trajectory (Calibrated to 12-Nov-2026 Close)</span>
            </div>
            <div style="display: flex; gap: 4px;">
              <button class="tf-btn" onclick="setTimeframe('1M', this)">1M (21d)</button>
              <button class="tf-btn active" onclick="setTimeframe('3M', this)">3M (63d)</button>
              <button class="tf-btn" onclick="setTimeframe('6M', this)">6M (126d)</button>
              <button class="tf-btn" onclick="setTimeframe('1Y', this)">1Y (252d)</button>
            </div>
          </div>

          <!-- TECHNICAL MOMENTUM INDICATORS SELECTOR (RS, RSI, MACD, ALL) -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; border-top: 1px solid var(--border-subtle); padding-top: 6px;">
            <div style="font-size: 10px; font-weight: 700; color: var(--cyan-rs); text-transform: uppercase;">Technical Momentum Signals:</div>
            <div style="display: flex; gap: 4px;">
              <button class="ind-btn ${selectedIndicator === 'RS' ? 'active' : ''}" onclick="setIndicator('RS', this)">RS vs N500</button>
              <button class="ind-btn ${selectedIndicator === 'RSI' ? 'active' : ''}" onclick="setIndicator('RSI', this)">RSI (14)</button>
              <button class="ind-btn ${selectedIndicator === 'MACD' ? 'active' : ''}" onclick="setIndicator('MACD', this)">MACD (12,26,9)</button>
              <button class="ind-btn ${selectedIndicator === 'ALL' ? 'active' : ''}" onclick="setIndicator('ALL', this)">All Signals</button>
            </div>
          </div>

          <canvas id="candlestick-canvas" style="display: block; cursor: crosshair;"></canvas>
        </div>

        <!-- QUANTITATIVE RISK & BENCHMARK MATRIX (252-DAY LOG-RETURN MODEL) -->
        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; flex-wrap: wrap; gap: 8px;">
            <div>
              <div style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Quantitative Risk & Benchmark Statistics (252-Day Log-Return Model)</div>
              <div style="font-size: 11px; color: var(--text-secondary); margin-top: 2px;">Side-by-side volatility annualized with √252. 1M (21-day) metric is subject to small-sample estimation noise.</div>
            </div>
            <!-- Benchmark Selector Toggle -->
            <div style="display: flex; gap: 4px; align-items: center;">
              <span style="font-size: 10px; color: var(--text-tertiary); font-weight: 700;">BENCHMARK:</span>
              <button class="theme-btn active" id="btn-bm-relevant" style="padding: 3px 8px; font-size: 10px;" onclick="switchBenchmarkInModal('relevant')">${relevantBmName} (Primary)</button>
              <button class="theme-btn" id="btn-bm-n500" style="padding: 3px 8px; font-size: 10px;" onclick="switchBenchmarkInModal('n500')">Nifty 500 (Broad)</button>
            </div>
          </div>

          <!-- SIDE-BY-SIDE VOLATILITY (SD) -->
          <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 12px;">
            <div class="stat-box">
              <div class="stat-box-title">1Y Volatility (252 Days)</div>
              <div class="stat-box-val" id="sd-1y-val">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Annualized with √252</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">6M Volatility (126 Days)</div>
              <div class="stat-box-val" id="sd-6m-val">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Annualized with √252</div>
            </div>
            <div class="stat-box" style="border-color: var(--amber);">
              <div class="stat-box-title" style="color: var(--amber);">1M Volatility (21 Days • Noisy)</div>
              <div class="stat-box-val" id="sd-1m-val" style="color: var(--amber);">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">~21 observations (sampling noise)</div>
            </div>
          </div>

          <!-- SUMMARY KPI ROW -->
          <div class="stat-badge-grid" style="margin-bottom: 12px;">
            <div class="stat-box">
              <div class="stat-box-title">Beta (<span id="kpi-bm-label">vs ${relevantBmName}</span>)</div>
              <div class="stat-box-val" id="kpi-quant-beta">...</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Correlation (ρ)</div>
              <div class="stat-box-val" id="kpi-quant-corr">...</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Annualized Covariance</div>
              <div class="stat-box-val" id="kpi-quant-cov">...</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">3M Alpha vs BM</div>
              <div class="stat-box-val ${parseFloat(alpha) >= 0 ? 'up' : 'down'}">${parseFloat(alpha) >= 0 ? '+' : ''}${alpha}%</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sharpe Ratio</div>
              <div class="stat-box-val up">${stock.quant.sharpe}</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sortino Ratio</div>
              <div class="stat-box-val up">${stock.quant.sortino}</div>
            </div>
          </div>

          <!-- COMPREHENSIVE STOCK VS BENCHMARK COMPARISON TABLE -->
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; font-size: 11px; font-family: var(--font-mono);">
              <thead>
                <tr style="border-bottom: 1px solid var(--border); color: var(--text-secondary); text-align: left;">
                  <th style="padding: 6px 8px;">Metric</th>
                  <th style="padding: 6px 8px; text-align: right;">1Y Window (252d)</th>
                  <th style="padding: 6px 8px; text-align: right;">6M Window (126d)</th>
                  <th style="padding: 6px 8px; text-align: right; color: var(--amber);">1M Window (21d • Noisy)</th>
                </tr>
              </thead>
              <tbody id="quant-comparison-table-body">
                <!-- Rendered dynamically -->
              </tbody>
            </table>
          </div>

          <div style="margin-top: 10px; padding-top: 8px; border-top: 1px dashed var(--border-subtle); font-size: 10px; color: var(--text-tertiary); line-height: 1.4;">
            ℹ️ <strong>CORS & Data Pipeline Architecture:</strong> Browsers cannot query NSE directly due to cross-origin resource sharing (CORS) enforcement. Pulling real-time exchange bhavcopy and sub-second order book telemetry requires routing through an authorized broker API or serverless backend proxy.
          </div>
        </div>

        <!-- TECHNICAL ANALYSIS STACK -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 12px 14px; margin-bottom: 14px;">
          <h4 style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 700; margin-bottom: 4px;">Technical Moving Average Alignment</h4>
          <p style="font-size: 12px; font-family: var(--font-mono); color: var(--text-primary);">${stock.maStack}</p>
        </div>

        <!-- Q2 FY27 CONCALL & MANAGEMENT HIGHLIGHTS -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 14px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <h4 style="font-size: 11px; color: var(--green); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Concall Key Highlights (${stock.quarter} • ${stock.concallDate})</h4>
            <span style="font-size: 9px; font-weight: 700; color: var(--cyan-rs); background: var(--cyan-rs-bg); padding: 2px 6px; border-radius: 4px;">EXCHANGE FILING & CONCALL SUMMARY</span>
          </div>
          <ul style="padding-left: 18px; font-size: 12px; color: var(--text-secondary); line-height: 1.6;">
            ${stock.takeaways.map(t => `<li style="margin-bottom: 4px;">${t}</li>`).join('')}
          </ul>

          <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-subtle);">
            <div style="font-size: 11px; font-weight: 700; color: var(--green);">MANAGEMENT GUIDANCE (FY27/FY28):</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${stock.guidance}</div>
          </div>

          <div style="margin-top: 8px;">
            <div style="font-size: 11px; font-weight: 700; color: var(--amber);">CAPEX & EXPANSION PIPELINE:</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${stock.capex}</div>
          </div>
        </div>

        <!-- DIRECT ONE-CLICK EXTERNAL LINKS -->
        <div style="display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap;">
          <a href="https://www.screener.in/company/${stock.symbol}/consolidated/" target="_blank" class="theme-btn" style="text-decoration:none;">📄 Open on Screener.in</a>
          <a href="https://www.nseindia.com/get-quotes/equity?symbol=${stock.symbol}" target="_blank" class="theme-btn" style="text-decoration:none;">🏛️ Official NSE Quote</a>
          <a href="https://www.bseindia.com/corporates/ann.html" target="_blank" class="theme-btn" style="text-decoration:none;">📑 BSE Filings & Transcripts</a>
        </div>
      `;

      document.getElementById("stock-modal").classList.add("active");

      // Initial quant calculation
      renderQuantSection(stock, 'relevant');

      setTimeout(() => {
        drawStockChart();
        const canvas = document.getElementById("candlestick-canvas");
        if (canvas) {
          canvas.addEventListener("mousemove", (e) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            let candles = activeStockCandles;
            if (selectedTimeframe === '1M') candles = candles.slice(-21);
            else if (selectedTimeframe === '3M') candles = candles.slice(-63);
            else if (selectedTimeframe === '6M') candles = candles.slice(-126);
            else if (selectedTimeframe === '1Y') candles = candles.slice(-252);
            const slotW = rect.width / candles.length;
            const idx = Math.floor(mouseX / slotW);
            drawStockChart(idx);
          });
          canvas.addEventListener("mouseleave", () => {
            drawStockChart(null);
          });
        }
      }, 50);
    }

    function closeModal(e) {
      if (e.target.id === "stock-modal") closeModalDirect();
    }
    function closeModalDirect() {
      document.getElementById("stock-modal").classList.remove("active");
      activeStockSymbol = '';
    }
'''

with open("web/index.html", "r", encoding="utf-8") as f:
    html = f.read()

start_pattern = r"    // CANDLESTICK CHART ENGINE WITH RSI & MACD INDICATORS"
end_pattern = r"    // INDICES MATRIX"

start_idx = html.find(start_pattern)
end_idx = html.find(end_pattern)

if start_idx == -1 or end_idx == -1:
    print(f"Could not find boundaries: start_idx={start_idx}, end_idx={end_idx}")
    exit(1)

html_updated = html[:start_idx] + new_engine_code + "\n\n" + html[end_idx:]

with open("web/index.html", "w", encoding="utf-8") as f:
    f.write(html_updated)

print("Successfully replaced chart & quant engine block!")
