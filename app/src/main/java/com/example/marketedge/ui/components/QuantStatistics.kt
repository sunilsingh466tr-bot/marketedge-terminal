package com.example.marketedge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.StockQuantStats
import com.example.ui.theme.*
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun StockQuantStatistics(
    stats: StockQuantStats,
    currentPrice: Double,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("quant_statistics_card")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = CyanRS,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "QUANTITATIVE RISK & STATISTICAL PROFILE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Standard Deviation (σ), Benchmark Correlation (r) & Factor Alpha",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanRSDark)
                    .border(1.dp, CyanRS.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "QUANT LAB",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 1: Standard Deviation (Volatility) KPIs
        Text(
            text = "1. STANDARD DEVIATION (σ) & VOLATILITY DISPERSION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantMetricPill(
                title = "Annual Volatility (σ)",
                value = "${String.format("%.1f", stats.standardDeviationAnnualized)}%",
                subtext = "Annualized dispersion",
                tint = if (stats.standardDeviationAnnualized > 28) BearishRed else AmberGold,
                modifier = Modifier.weight(1f)
            )
            QuantMetricPill(
                title = "Daily Volatility (σ)",
                value = "${String.format("%.2f", stats.standardDeviationDaily)}%",
                subtext = "Daily standard deviation",
                tint = CyanRS,
                modifier = Modifier.weight(1f)
            )
            QuantMetricPill(
                title = "30-Day Rolling (σ)",
                value = "${String.format("%.1f", stats.standardDeviation30D)}%",
                subtext = "Recent session volatility",
                tint = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bell Curve Gaussian Normal Distribution Visualization Canvas
        NormalDistributionCanvas(
            currentPrice = currentPrice,
            oneSigmaLow = stats.oneSigmaPriceLow,
            oneSigmaHigh = stats.oneSigmaPriceHigh,
            twoSigmaLow = stats.twoSigmaPriceLow,
            twoSigmaHigh = stats.twoSigmaPriceHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sigma Price Cones Table
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("1-Sigma Cone (68% Prob):", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", stats.oneSigmaPriceLow)} – ₹${String.format("%,.0f", stats.oneSigmaPriceHigh)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("2-Sigma Cone (95% Extreme):", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", stats.twoSigmaPriceLow)} – ₹${String.format("%,.0f", stats.twoSigmaPriceHigh)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AmberGold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: Correlation Analysis (r)
        Text(
            text = "2. PEARSON CORRELATION MATRIX (r)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Measures relational movement (-1.00 Inverse to +1.00 Lockstep)",
            fontSize = 9.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        CorrelationRow(
            benchmarkName = "Correlation vs Nifty 500 (Benchmark)",
            correlationValue = stats.correlationNifty500,
            description = if (stats.correlationNifty500 >= 0.70) "High Market Beta Alignment" else "Moderate Decoupling (High Alpha Potential)"
        )
        Spacer(modifier = Modifier.height(6.dp))
        CorrelationRow(
            benchmarkName = "Correlation vs Sector Index",
            correlationValue = stats.correlationSector,
            description = "Sector Theme Cohesion"
        )
        Spacer(modifier = Modifier.height(6.dp))
        CorrelationRow(
            benchmarkName = "Correlation vs Nifty 50 (Large-Cap)",
            correlationValue = stats.correlationNifty50,
            description = "Diversification against Large-Cap Stagnation"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: Factor Ratios (Beta, Sharpe, Sortino, R²)
        Text(
            text = "3. SYSTEMATIC RISK & RISK-ADJUSTED ALPHA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantMetricPill(
                title = "Beta (β vs Nifty 500)",
                value = "${String.format("%.2f", stats.betaNifty500)}x",
                subtext = if (stats.betaNifty500 > 1.0) "High-Beta Outperformer" else "Low-Beta Defensive",
                tint = if (stats.betaNifty500 > 1.2) AmberGold else CyanRS,
                modifier = Modifier.weight(1f)
            )
            QuantMetricPill(
                title = "Sharpe Ratio (rf 6.8%)",
                value = String.format("%.2f", stats.sharpeRatio),
                subtext = if (stats.sharpeRatio >= 2.0) "Excellent Quality" else "Adequate Risk-Reward",
                tint = if (stats.sharpeRatio >= 2.0) BullishGreen else TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantMetricPill(
                title = "Sortino Ratio",
                value = String.format("%.2f", stats.sortinoRatio),
                subtext = "Downside Risk Protection",
                tint = BullishGreen,
                modifier = Modifier.weight(1f)
            )
            QuantMetricPill(
                title = "R-Squared (R²)",
                value = "${String.format("%.1f", stats.rSquaredPercent)}%",
                subtext = "${String.format("%.0f", 100.0 - stats.rSquaredPercent)}% Idiosyncratic Alpha",
                tint = CyanRS,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quantitative Synthesis Insight Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCardElevated)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CyanRS,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quantitative Takeaway: $symbol exhibits an annualized σ of ${String.format("%.1f", stats.standardDeviationAnnualized)}% with a correlation of r = ${String.format("%.2f", stats.correlationNifty500)} to Nifty 500. Its Sortino ratio of ${String.format("%.2f", stats.sortinoRatio)} confirms that volatility is predominantly driven by upward expansions rather than downside capital erosion.",
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuantMetricPill(
    title: String,
    value: String,
    subtext: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = title, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = tint
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(text = subtext, fontSize = 8.sp, color = TextTertiary, maxLines = 1)
    }
}

@Composable
private fun CorrelationRow(
    benchmarkName: String,
    correlationValue: Double,
    description: String,
    modifier: Modifier = Modifier
) {
    val isPositive = correlationValue >= 0
    val corrFraction = ((correlationValue + 1.0) / 2.0).toFloat().coerceIn(0f, 1f) // Maps -1..+1 to 0..1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalCard)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = benchmarkName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = description, fontSize = 8.sp, color = TextSecondary)
            }

            Text(
                text = "r = ${if (isPositive) "+" else ""}${String.format("%.2f", correlationValue)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (correlationValue > 0.65) CyanRS else BullishGreen
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Horizontal Correlation Slider Gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TerminalCardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(corrFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(BearishRed.copy(alpha = 0.5f), AmberGold, CyanRS)
                        )
                    )
            )
        }
    }
}

/**
 * Canvas drawing a Gaussian Bell Curve with Standard Deviation intervals
 */
@Composable
private fun NormalDistributionCanvas(
    currentPrice: Double,
    oneSigmaLow: Double,
    oneSigmaHigh: Double,
    twoSigmaLow: Double,
    twoSigmaHigh: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path()
        val fillPath = Path()
        val pointsCount = 60
        val meanIndex = pointsCount / 2

        for (i in 0..pointsCount) {
            val x = (i.toFloat() / pointsCount) * width
            // Standard normal distribution curve formula: exp(-0.5 * z^2)
            val z = ((i - meanIndex).toFloat() / (pointsCount / 6f))
            val yNorm = exp(-0.5 * z.pow(2)).toFloat()
            val y = height - (yNorm * (height * 0.85f)) - 5f

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw soft gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyanRS.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw curve stroke
        drawPath(
            path = path,
            color = CyanRS,
            style = Stroke(width = 2.2f)
        )

        // Draw Center Mean Price Line
        val centerX = width / 2f
        drawLine(
            color = BullishGreen,
            start = Offset(centerX, 8f),
            end = Offset(centerX, height),
            strokeWidth = 2f
        )

        // Draw 1-Sigma Bounds (dashed effect or thin lines)
        val oneSigmaLeftX = width * 0.33f
        val oneSigmaRightX = width * 0.67f
        drawLine(
            color = AmberGold.copy(alpha = 0.7f),
            start = Offset(oneSigmaLeftX, height * 0.35f),
            end = Offset(oneSigmaLeftX, height),
            strokeWidth = 1.2f
        )
        drawLine(
            color = AmberGold.copy(alpha = 0.7f),
            start = Offset(oneSigmaRightX, height * 0.35f),
            end = Offset(oneSigmaRightX, height),
            strokeWidth = 1.2f
        )
    }
}
