package com.example.marketedge.model

enum class MarketTag(val displayName: String) {
    INSTITUTIONAL("FII / DII"),
    MACRO("Macro / Rates"),
    SECTORAL("Top Sectors"),
    RESULTS("Earnings"),
    ANNOUNCEMENT("Corporate Action"),
    ORDER_WIN("Orders / Capex")
}

enum class MarketImpact {
    BULLISH,
    BEARISH,
    NEUTRAL
}

data class FiiDiiPoint(
    val date: String,
    val fiiNetCr: Double, // in Crores (+ is net buy, - is net sell)
    val diiNetCr: Double
) {
    val totalNetCr: Double get() = fiiNetCr + diiNetCr
}

data class MarketBullet(
    val id: String,
    val title: String,
    val tag: MarketTag,
    val description: String,
    val impact: MarketImpact,
    val source: String = "Terminal Feed"
)

data class CapBreadth(
    val category: String, // "Large-cap (Top 100)", "Mid-cap (101-250)", "Small-cap (251-500)"
    val advances: Int,
    val declines: Int,
    val percentAbove50Ema: Int
) {
    val advanceRatio: Float
        get() = if (declines > 0) advances.toFloat() / declines else advances.toFloat()
}

data class MarketBreadth(
    val advances: Int,
    val declines: Int,
    val unchanged: Int,
    val upAbove5Percent: Int,
    val upAbove10Percent: Int,
    val upperCircuit20: Int,
    val lowerCircuit: Int = 56,
    val highs52W: Int,
    val lows52W: Int,
    val registeredAccounts: String = "27,65,97,533",
    val marketCap: String = "₹506.65 Lac Cr ($5.40 Tn)",
    val latencyNs: Int = 548,
    val largeCapBreadth: CapBreadth,
    val midCapBreadth: CapBreadth,
    val smallCapBreadth: CapBreadth
) {
    val totalStocks: Int get() = advances + declines + unchanged
    val advancePercent: Float get() = if (totalStocks > 0) (advances.toFloat() / totalStocks) * 100f else 50f
}
