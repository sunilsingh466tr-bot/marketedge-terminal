package com.example.marketedge.model

data class CandleData(
    val dateStr: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long,
    val ema21: Float,
    val sma50: Float,
    val sma200: Float,
    val rsValue: Float, // Relative Strength line vs Nifty 500
    val rsi: Float = 50f, // 14-period Relative Strength Index (0 - 100)
    val macd: Float = 0f, // MACD line (12 EMA - 26 EMA)
    val macdSignal: Float = 0f, // MACD 9-period Signal line
    val macdHist: Float = 0f // MACD Histogram (MACD - Signal)
)

data class ConcallDossier(
    val latestQuarter: String = "Q2 FY26",
    val concallDate: String = "October 2026 (Recent)",
    val keyTakeaways: List<String> = emptyList(),
    val managementGuidance: String = "",
    val capexPlans: String = "",
    val audioUrl: String,
    val transcriptPdfUrl: String,
    val investorPptUrl: String,
    val annualReportUrl: String,
    val screenerInUrl: String,
    val nseUrl: String
)

data class StockQuantStats(
    val standardDeviationAnnualized: Double, // Annualized Volatility % (e.g. 26.4%)
    val standardDeviationDaily: Double,      // Daily Volatility % (e.g. 1.66%)
    val standardDeviation30D: Double,        // 30-day Rolling Volatility % (e.g. 21.8%)
    val correlationNifty500: Double,         // Pearson Correlation r vs Nifty 500 (-1.0 to +1.0)
    val correlationSector: Double,           // Correlation r vs Sector Index
    val correlationNifty50: Double,          // Correlation r vs Nifty 50
    val betaNifty500: Double,                // Systematic Risk Beta vs Nifty 500
    val alphaAnnualizedPercent: Double,      // Jensen's Alpha % excess return
    val sharpeRatio: Double,                 // Risk-adjusted Return (rf = 6.8%)
    val sortinoRatio: Double,                // Downside risk-adjusted return
    val rSquaredPercent: Double,             // % variance explained by benchmark
    val maxDrawdown1YPercent: Double,        // Peak-to-trough 1-year drawdown
    val oneSigmaPriceLow: Double,            // 1-Sigma (68% confidence) price band
    val oneSigmaPriceHigh: Double,
    val twoSigmaPriceLow: Double,            // 2-Sigma (95% confidence) price band
    val twoSigmaPriceHigh: Double
)

data class ScreenerStock(
    val symbol: String,
    val name: String,
    val sector: String,
    val marketCapCategory: String, // "Large-Cap", "Mid-Cap", "Small-Cap"
    val currentPrice: Double,
    val changePercent: Double,
    val high52W: Double,
    val low52W: Double,
    val distanceFrom52WHighPercent: Double, // e.g. -2.3%
    val threeMonthReturnPercent: Double,
    val nifty500ThreeMonthReturn: Double,
    val rsScore: Int, // 0 - 99 percentile ranking vs Nifty 500
    val ema21: Double,
    val sma50: Double,
    val sma200: Double,
    val volumeMultiplier: Float, // vs 20DMA
    val patterns: List<String>,
    val isNear52WHigh: Boolean, // within 5%
    val isResilientInCorrection: Boolean,
    val isPocketPivot: Boolean,
    val candles: List<CandleData>,
    val concall: ConcallDossier,
    val quantStats: StockQuantStats
)
