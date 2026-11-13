package com.example.marketedge.model

enum class IndexCategory(val label: String) {
    BROAD("Broad Market"),
    SECTORAL("Sectoral Indices")
}

data class IndexSummary(
    val id: String,
    val name: String,
    val category: IndexCategory,
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val sparklinePoints: List<Float>, // 52-week normalized price history for sparkline
    val high52W: Double,
    val low52W: Double,
    val distanceFrom50EmaPercent: Double,
    val distanceFrom200EmaPercent: Double,
    val trendStatus: String // "Strong Bullish", "Mild Bullish", "Consolidating", "Correction"
)
