package com.example.marketedge.model

enum class EarningsVerdict(val label: String) {
    STRONG_BEAT("Strong Beat"),
    IN_LINE("In-Line / Okay"),
    WEAK_MISS("Weak / Miss")
}

enum class VolatilityExpectation(val label: String, val level: Int) {
    HIGH("High Volatility", 3),
    MEDIUM("Moderate Volatility", 2),
    LOW("Low Volatility", 1)
}

data class UpcomingEarnings(
    val symbol: String,
    val companyName: String,
    val sector: String,
    val expectedDate: String, // e.g. "10 Nov (Tue)", "Tomorrow"
    val dayOfWeek: String,    // "Mon", "Tue", "Wed", "Thu", "Fri"
    val marketCapCategory: String, // "Large-Cap", "Mid-Cap", "Small-Cap"
    val estimatedRevenueCr: Double,
    val estimatedPatCr: Double,
    val volatility: VolatilityExpectation,
    val impliedMovePercent: Double, // Options implied move +/- %
    val historicalReactionPercent: Double, // Avg historical reaction +/- %
    val keyFocus: String, // e.g. "Order book inflows & GCC pipeline"
    val timeSlot: String = "Post-Market Close" // "Post-Market Close", "Market Hours", "Pre-Market Open"
)

data class EarningsResult(
    val symbol: String,
    val companyName: String,
    val sector: String,
    val quarter: String,
    val revenueCr: Double,
    val revenueYoy: Double,
    val revenueQoq: Double,
    val patCr: Double,
    val patYoy: Double,
    val patQoq: Double,
    val verdict: EarningsVerdict,
    val priceReactionPercent: Double,
    val volumeMultiplier: Float, // e.g. 4.8x of 20DMA
    val marketRemark: String,
    val isUpperCircuit: Boolean = false
)
