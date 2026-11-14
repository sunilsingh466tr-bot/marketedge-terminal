/**
 * lib/quant.js
 * Quantitative & Technical Indicator Engine computed on backend from real daily EOD bars.
 */

function logRet(prices) {
  const res = [];
  for (let i = 1; i < prices.length; i++) {
    if (prices[i - 1] > 0 && prices[i] > 0) {
      res.push(Math.log(prices[i] / prices[i - 1]));
    }
  }
  return res;
}

function mean(arr) {
  if (!arr.length) return 0;
  return arr.reduce((s, x) => s + x, 0) / arr.length;
}

function cov(x, y) {
  const n = Math.min(x.length, y.length);
  if (n < 2) return 0;
  const xs = x.slice(-n);
  const ys = y.slice(-n);
  const mx = mean(xs);
  const my = mean(ys);
  let sum = 0;
  for (let i = 0; i < n; i++) {
    sum += (xs[i] - mx) * (ys[i] - my);
  }
  return sum / (n - 1);
}

function sd(arr) {
  return Math.sqrt(Math.max(0, cov(arr, arr)));
}

/**
 * Aligns stock bars and index bars by exact trading date (YYYY-MM-DD),
 * using split/bonus/dividend ADJUSTED close prices (`adjC ?? c`) for all returns, SD, beta & covariance.
 */
function alignSeriesByDate(stockBars, indexBars) {
  const idxMap = new Map();
  for (const b of indexBars) {
    const closeVal = b ? (b.adjC ?? b.c) : null;
    if (b && b.date && closeVal != null && closeVal > 0) {
      idxMap.set(b.date, closeVal);
    }
  }

  const dates = [];
  const stockCloses = [];
  const indexCloses = [];

  for (const b of stockBars) {
    const stockCloseVal = b ? (b.adjC ?? b.c) : null;
    if (b && b.date && stockCloseVal != null && stockCloseVal > 0 && idxMap.has(b.date)) {
      dates.push(b.date);
      stockCloses.push(stockCloseVal);
      indexCloses.push(idxMap.get(b.date));
    }
  }

  return { dates, stockCloses, indexCloses };
}

/**
 * Computes window statistics for `days` return observations (252, 126, 63, or 21).
 */
function computeWindowStats(stockCloses, indexCloses, days) {
  const neededCloses = Math.min(stockCloses.length, indexCloses.length);
  if (neededCloses < 5) {
    return null;
  }

  const sliceLen = Math.min(neededCloses, days + 1);
  const sSlice = stockCloses.slice(-sliceLen);
  const iSlice = indexCloses.slice(-sliceLen);

  const sRetSeries = logRet(sSlice);
  const iRetSeries = logRet(iSlice);
  const obsCount = Math.min(sRetSeries.length, iRetSeries.length);
  if (obsCount < 2) return null;

  const c = cov(sRetSeries, iRetSeries);
  const sSD = sd(sRetSeries);
  const iSD = sd(iRetSeries);

  const sdStockAnn = sSD * Math.sqrt(252) * 100;
  const sdIdxAnn = iSD * Math.sqrt(252) * 100;
  const covAnn = c * 252;
  const corr = (sSD > 0 && iSD > 0) ? c / (sSD * iSD) : 0;
  const beta = iSD > 0 ? c / (iSD ** 2) : 1;

  const stockReturn = ((sSlice[sSlice.length - 1] / sSlice[0]) - 1) * 100;
  const indexReturn = ((iSlice[iSlice.length - 1] / iSlice[0]) - 1) * 100;
  const alphaExcess = stockReturn - indexReturn;
  const jensenAlpha = stockReturn - beta * indexReturn;

  return {
    daysRequested: days,
    observations: obsCount,
    isNoisy: obsCount <= 25,
    stockReturn: Number(stockReturn.toFixed(2)),
    indexReturn: Number(indexReturn.toFixed(2)),
    sdStock: Number(sdStockAnn.toFixed(2)),
    sdIdx: Number(sdIdxAnn.toFixed(2)),
    covAnn: Number(covAnn.toFixed(6)),
    covAnnPct: Number((covAnn * 100).toFixed(2)),
    corr: Number(corr.toFixed(3)),
    beta: Number(beta.toFixed(3)),
    alpha: Number(alphaExcess.toFixed(2)),
    jensenAlpha: Number(jensenAlpha.toFixed(2))
  };
}

function computeBenchmarkComparison(stockBars, benchmarkBars) {
  const { dates, stockCloses, indexCloses } = alignSeriesByDate(stockBars, benchmarkBars);
  if (dates.length < 22) return null;

  return {
    alignedDays: dates.length,
    startDate: dates[0],
    endDate: dates[dates.length - 1],
    '1Y': computeWindowStats(stockCloses, indexCloses, 252),
    '6M': computeWindowStats(stockCloses, indexCloses, 126),
    '3M': computeWindowStats(stockCloses, indexCloses, 63),
    '1M': computeWindowStats(stockCloses, indexCloses, 21)
  };
}

function simpleReturnOverBars(closes, tradingDaysBack) {
  if (!closes || closes.length < 2) return null;
  const endIdx = closes.length - 1;
  const startIdx = Math.max(0, endIdx - tradingDaysBack);
  if (closes[startIdx] <= 0) return null;
  return ((closes[endIdx] / closes[startIdx]) - 1) * 100;
}

/**
 * Computes technical MAs, RSI(14), MACD(12,26,9), Volume ratio, and 52W range from real OHLCV bars.
 */
function computeTechnicals(bars) {
  if (!bars || bars.length === 0) return null;
  const closes = bars.map(b => b.c);
  const adjCloses = bars.map(b => b.adjC ?? b.c);
  const n = closes.length;
  const latest = bars[n - 1];
  const prev = n >= 2 ? bars[n - 2] : latest;

  // 21 EMA
  const k21 = 2 / 22;
  let ema21 = closes[0];
  for (let i = 1; i < n; i++) {
    ema21 = closes[i] * k21 + ema21 * (1 - k21);
  }

  // 50 SMA & 200 SMA
  const slice50 = closes.slice(-Math.min(50, n));
  const sma50 = mean(slice50);
  const slice200 = closes.slice(-Math.min(200, n));
  const sma200 = mean(slice200);

  // RSI (14) Wilder's smoothing
  let rsi14 = 50;
  if (n > 14) {
    let avgGain = 0;
    let avgLoss = 0;
    for (let i = 1; i <= 14; i++) {
      const diff = closes[i] - closes[i - 1];
      if (diff > 0) avgGain += diff;
      else avgLoss += -diff;
    }
    avgGain /= 14;
    avgLoss /= 14;
    for (let i = 15; i < n; i++) {
      const diff = closes[i] - closes[i - 1];
      const g = diff > 0 ? diff : 0;
      const l = diff < 0 ? -diff : 0;
      avgGain = (avgGain * 13 + g) / 14;
      avgLoss = (avgLoss * 13 + l) / 14;
    }
    if (avgLoss === 0) rsi14 = 100;
    else {
      const rs = avgGain / avgLoss;
      rsi14 = 100 - 100 / (1 + rs);
    }
  }

  // MACD (12, 26, 9)
  const k12 = 2 / 13;
  const k26 = 2 / 27;
  const k9 = 2 / 10;
  let e12 = closes[0];
  let e26 = closes[0];
  let sig = 0;
  let macdLine = 0;
  for (let i = 0; i < n; i++) {
    e12 = closes[i] * k12 + e12 * (1 - k12);
    e26 = closes[i] * k26 + e26 * (1 - k26);
    macdLine = e12 - e26;
    sig = i === 0 ? macdLine : macdLine * k9 + sig * (1 - k9);
  }
  const macdHist = macdLine - sig;

  // Volume vs 20DMA
  const recentBars20 = bars.slice(-Math.min(21, n));
  const priorVols = recentBars20.slice(0, -1).map(b => b.v || 0).filter(v => v > 0);
  const avg20Vol = priorVols.length ? mean(priorVols) : (latest.v || 0);
  const volRatio = avg20Vol > 0 && latest.v > 0 ? latest.v / avg20Vol : 1;

  // 52W High / Low from bars (approx 252 trading days)
  const yearBars = bars.slice(-Math.min(252, n));
  let high52 = -Infinity;
  let low52 = Infinity;
  for (const b of yearBars) {
    if (b.h > high52) high52 = b.h;
    if (b.l < low52) low52 = b.l;
  }

  const chg = latest.c - prev.c;
  const chgPct = prev.c > 0 ? (chg / prev.c) * 100 : 0;

  return {
    price: Number(latest.c.toFixed(2)),
    prevClose: Number(prev.c.toFixed(2)),
    chg: Number(chg.toFixed(2)),
    chgPct: Number(chgPct.toFixed(2)),
    isUpper: chgPct >= 19.5, // Numeric price-band check, never string-matching
    high52: Number(high52.toFixed(2)),
    low52: Number(low52.toFixed(2)),
    ema21: Number(ema21.toFixed(2)),
    sma50: Number(sma50.toFixed(2)),
    sma200: Number(sma200.toFixed(2)),
    rsi14: Number(rsi14.toFixed(1)),
    macd: Number(macdLine.toFixed(2)),
    macdSig: Number(sig.toFixed(2)),
    macdHist: Number(macdHist.toFixed(2)),
    volRatio: Number(volRatio.toFixed(2)),
    latestVolume: latest.v || 0,
    ret3M: Number((simpleReturnOverBars(adjCloses, 63) ?? 0).toFixed(2)),
    ret6M: Number((simpleReturnOverBars(adjCloses, 126) ?? 0).toFixed(2)),
    ret9M: Number((simpleReturnOverBars(adjCloses, 189) ?? 0).toFixed(2)),
    ret12M: Number((simpleReturnOverBars(adjCloses, 252) ?? 0).toFixed(2))
  };
}

/**
 * Computes RS Score as a percentile rank (1..99) across the entire stock universe
 * using weighted 3/6/9/12-month returns with weights 40% / 20% / 20% / 20%.
 */
function assignUniverseRSPercentiles(stockEntries) {
  const scored = stockEntries.map(s => {
    const r3 = s.ret3M ?? 0;
    const r6 = s.ret6M ?? 0;
    const r9 = s.ret9M ?? 0;
    const r12 = s.ret12M ?? 0;
    const weightedScore = 0.40 * r3 + 0.20 * r6 + 0.20 * r9 + 0.20 * r12;
    return { symbol: s.symbol, weightedScore };
  });

  scored.sort((a, b) => a.weightedScore - b.weightedScore);
  const total = scored.length;
  const rsMap = new Map();

  for (let i = 0; i < total; i++) {
    const pctRank = total <= 1 ? 50 : Math.round(1 + (i / (total - 1)) * 98);
    rsMap.set(scored[i].symbol, {
      rs: pctRank,
      weightedMomentum: Number(scored[i].weightedScore.toFixed(2))
    });
  }

  for (const s of stockEntries) {
    const rankObj = rsMap.get(s.symbol);
    s.rs = rankObj.rs;
    s.weightedMomentum = rankObj.weightedMomentum;
  }
}

module.exports = {
  logRet,
  mean,
  cov,
  sd,
  alignSeriesByDate,
  computeWindowStats,
  computeBenchmarkComparison,
  computeTechnicals,
  assignUniverseRSPercentiles
};
