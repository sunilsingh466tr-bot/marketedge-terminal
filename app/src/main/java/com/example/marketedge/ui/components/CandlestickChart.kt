package com.example.marketedge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.CandleData
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun CandlestickChart(
    candles: List<CandleData>,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTimeframe by remember { mutableStateOf("3M") }
    var activeIndicatorTab by remember { mutableStateOf("RSI") } // "RS", "RSI", "MACD", "ALL"

    val activeCandles = remember(candles, selectedTimeframe) {
        val filtered = when (selectedTimeframe) {
            "1M" -> candles.takeLast(12)
            "3M" -> candles.takeLast(20)
            "6M" -> candles
            "1Y" -> candles
            else -> candles
        }

        // Ensure RSI and MACD are populated
        if (filtered.isNotEmpty() && filtered.all { it.rsi == 50f && it.macd == 0f }) {
            computeMissingIndicators(filtered)
        } else {
            filtered
        }
    }

    val currentInspected = selectedIndex?.let { activeCandles.getOrNull(it) } ?: activeCandles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Timeframe selector & Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("1M", "3M", "6M", "1Y").forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanRSDark else TerminalCard)
                            .border(
                                1.dp,
                                if (isSelected) CyanRS else TerminalBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .testTag("timeframe_${tf}")
                            .pointerInput(tf) {
                                detectTapGestures {
                                    selectedTimeframe = tf
                                    selectedIndex = null
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tf,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyanRS else TextSecondary
                        )
                    }
                }
            }

            // MA Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(CyanRS, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("21 EMA", fontSize = 9.sp, color = CyanRS)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(AmberGold, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("50 SMA", fontSize = 9.sp, color = AmberGold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(PurpleMA, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("200 SMA", fontSize = 9.sp, color = PurpleMA)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HUD / Inspector banner
        if (currentInspected != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalCard)
                    .border(1.dp, TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentInspected.dateStr,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "O:${String.format("%.1f", currentInspected.open)} H:${String.format("%.1f", currentInspected.high)} L:${String.format("%.1f", currentInspected.low)} C:${String.format("%.1f", currentInspected.close)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (currentInspected.close >= currentInspected.open) BullishGreen else BearishRed
                    )
                    Text(
                        text = "RS:${String.format("%.0f", currentInspected.rsValue)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanRS
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rsiColor = when {
                        currentInspected.rsi >= 70f -> AmberGold
                        currentInspected.rsi <= 30f -> BullishGreen
                        currentInspected.rsi >= 60f -> CyanRS
                        else -> TextSecondary
                    }
                    val rsiStatus = when {
                        currentInspected.rsi >= 70f -> "Overbought"
                        currentInspected.rsi <= 30f -> "Oversold"
                        currentInspected.rsi >= 60f -> "Bullish Zone"
                        else -> "Neutral"
                    }
                    Text(
                        text = "RSI(14): ${String.format("%.1f", currentInspected.rsi)} ($rsiStatus)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = rsiColor
                    )

                    val isMacdBull = currentInspected.macdHist >= 0
                    Text(
                        text = "MACD: ${String.format("%.1f", currentInspected.macd)} | Sig: ${String.format("%.1f", currentInspected.macdSignal)} | Hist: ${String.format("%+.1f", currentInspected.macdHist)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isMacdBull) BullishGreen else BearishRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Candlestick + MA Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(activeCandles) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val slotW = size.width / activeCandles.size
                            selectedIndex = (offset.x / slotW).toInt().coerceIn(0, activeCandles.size - 1)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val slotW = size.width / activeCandles.size
                            selectedIndex = (change.position.x / slotW).toInt().coerceIn(0, activeCandles.size - 1)
                        },
                        onDragEnd = { /* keep last */ }
                    )
                }
                .pointerInput(activeCandles) {
                    detectTapGestures { offset ->
                        val slotW = size.width / activeCandles.size
                        selectedIndex = (offset.x / slotW).toInt().coerceIn(0, activeCandles.size - 1)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val n = activeCandles.size
            if (n < 2) return@Canvas

            val slotWidth = width / n

            // Calculate min and max for price pane (leave bottom 20% for volume)
            val candleMax = activeCandles.maxOf { it.high }
            val candleMin = activeCandles.minOf { it.low }
            val priceRange = (candleMax - candleMin).coerceAtLeast(1f)
            val chartTopY = 10f
            val chartBottomY = height * 0.72f
            val chartAvailableH = chartBottomY - chartTopY

            fun priceToY(price: Float): Float {
                val ratio = (price - candleMin) / priceRange
                return chartBottomY - (ratio * chartAvailableH)
            }

            // Draw horizontal grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val gridY = chartTopY + (chartAvailableH * fraction)
                drawLine(
                    color = DividerColor.copy(alpha = 0.5f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }

            // Path for 21 EMA, 50 SMA, 200 SMA
            val emaPath = Path()
            val sma50Path = Path()
            val sma200Path = Path()

            // Draw Candlesticks & build MA paths
            activeCandles.forEachIndexed { i, candle ->
                val centerX = (i * slotWidth) + (slotWidth / 2)
                val bodyW = (slotWidth * 0.65f).coerceAtLeast(4f)
                val isBull = candle.close >= candle.open
                val candleColor = if (isBull) BullishGreen else BearishRed

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.6f
                )

                // Candle Body
                val bodyTop = minOf(openY, closeY)
                val bodyH = maxOf(abs(openY - closeY), 2f)
                drawRect(
                    color = candleColor,
                    topLeft = Offset(centerX - (bodyW / 2), bodyTop),
                    size = Size(bodyW, bodyH)
                )

                // Add to MA paths
                val emaY = priceToY(candle.ema21)
                val sma50Y = priceToY(candle.sma50)
                val sma200Y = priceToY(candle.sma200)

                if (i == 0) {
                    emaPath.moveTo(centerX, emaY)
                    sma50Path.moveTo(centerX, sma50Y)
                    sma200Path.moveTo(centerX, sma200Y)
                } else {
                    emaPath.lineTo(centerX, emaY)
                    sma50Path.lineTo(centerX, sma50Y)
                    sma200Path.lineTo(centerX, sma200Y)
                }
            }

            // Draw Moving Average Overlay Lines
            drawPath(path = emaPath, color = CyanRS, style = Stroke(width = 2f))
            drawPath(path = sma50Path, color = AmberGold, style = Stroke(width = 2f))
            drawPath(path = sma200Path, color = PurpleMA, style = Stroke(width = 1.8f))

            // Lower Volume Histogram Pane
            val maxVol = activeCandles.maxOf { it.volume }.coerceAtLeast(1L).toFloat()
            val volTopY = height * 0.78f
            val volH = height - volTopY

            activeCandles.forEachIndexed { i, candle ->
                val centerX = (i * slotWidth) + (slotWidth / 2)
                val barW = (slotWidth * 0.60f).coerceAtLeast(3f)
                val barH = (candle.volume.toFloat() / maxVol) * volH
                val isBull = candle.close >= candle.open
                val volColor = if (isBull) BullishGreen.copy(alpha = 0.5f) else BearishRed.copy(alpha = 0.5f)

                drawRect(
                    color = volColor,
                    topLeft = Offset(centerX - (barW / 2), height - barH),
                    size = Size(barW, barH)
                )
            }

            // Crosshair overlay if selected
            selectedIndex?.let { idx ->
                val centerX = (idx * slotWidth) + (slotWidth / 2)
                drawLine(
                    color = TextPrimary.copy(alpha = 0.5f),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, height),
                    strokeWidth = 1f
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INDICATOR SELECTION TABS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MOMENTUM INDICATORS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanRS,
                letterSpacing = 0.6.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "RS" to "RS Line",
                    "RSI" to "RSI (14)",
                    "MACD" to "MACD",
                    "ALL" to "All Signals"
                ).forEach { (key, label) ->
                    val isSelected = activeIndicatorTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CyanRSDark else TerminalCard)
                            .border(
                                1.dp,
                                if (isSelected) CyanRS else TerminalBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .testTag("ind_tab_${key}")
                            .pointerInput(key) {
                                detectTapGestures { activeIndicatorTab = key }
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyanRS else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SUB-PANE 1: RELATIVE STRENGTH (RS) VS NIFTY 500
        if (activeIndicatorTab == "RS" || activeIndicatorTab == "ALL") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RELATIVE STRENGTH (RS) VS NIFTY 500",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanRS
                )
                Text(
                    text = "Mansfield Curve (Up = Outperforming)",
                    fontSize = 8.5.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                val width = size.width
                val height = size.height
                val n = activeCandles.size
                if (n < 2) return@Canvas
                val slotWidth = width / n

                val rsPath = Path()
                val minRs = activeCandles.minOf { it.rsValue }
                val maxRs = activeCandles.maxOf { it.rsValue }
                val rangeRs = (maxRs - minRs).coerceAtLeast(1f)

                // Baseline
                val baseLineY = height * 0.5f
                drawLine(
                    color = DividerColor,
                    start = Offset(0f, baseLineY),
                    end = Offset(width, baseLineY),
                    strokeWidth = 1f
                )

                activeCandles.forEachIndexed { i, candle ->
                    val x = (i * slotWidth) + (slotWidth / 2)
                    val y = height - ((candle.rsValue - minRs) / rangeRs * (height * 0.8f)) - (height * 0.1f)
                    if (i == 0) rsPath.moveTo(x, y) else rsPath.lineTo(x, y)
                }

                drawPath(path = rsPath, color = CyanRS, style = Stroke(width = 2f))

                selectedIndex?.let { idx ->
                    val cx = (idx * slotWidth) + (slotWidth / 2)
                    drawLine(
                        color = TextPrimary.copy(alpha = 0.4f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, height),
                        strokeWidth = 1f
                    )
                }
            }
            if (activeIndicatorTab == "ALL") Spacer(modifier = Modifier.height(6.dp))
        }

        // SUB-PANE 2: RSI (14-PERIOD RELATIVE STRENGTH INDEX)
        if (activeIndicatorTab == "RSI" || activeIndicatorTab == "ALL") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RSI (14) MOMENTUM OSCILLATOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleMA
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val lastRsi = currentInspected?.rsi ?: 50f
                    val zoneTag = when {
                        lastRsi >= 70f -> "OVERBOUGHT (≥70)"
                        lastRsi <= 30f -> "OVERSOLD (≤30)"
                        lastRsi >= 60f -> "BULLISH EXPANSION"
                        else -> "RANGEBOUND"
                    }
                    Text(
                        text = "• $zoneTag",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastRsi >= 60f) BullishGreen else if (lastRsi <= 30f) AmberGold else TextSecondary
                    )
                }
                Text(
                    text = "70 OB | 30 OS",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                val width = size.width
                val height = size.height
                val n = activeCandles.size
                if (n < 2) return@Canvas
                val slotWidth = width / n

                // RSI range is 0 to 100
                fun rsiToY(v: Float): Float {
                    val clamped = v.coerceIn(0f, 100f)
                    return height - (clamped / 100f * height)
                }

                val y70 = rsiToY(70f)
                val y30 = rsiToY(30f)
                val y50 = rsiToY(50f)

                // 70 Overbought band (dashed red/amber)
                drawLine(
                    color = AmberGold.copy(alpha = 0.6f),
                    start = Offset(0f, y70),
                    end = Offset(width, y70),
                    strokeWidth = 1f
                )

                // 50 Midline (neutral)
                drawLine(
                    color = DividerColor.copy(alpha = 0.5f),
                    start = Offset(0f, y50),
                    end = Offset(width, y50),
                    strokeWidth = 1f
                )

                // 30 Oversold band (dashed green)
                drawLine(
                    color = BullishGreen.copy(alpha = 0.6f),
                    start = Offset(0f, y30),
                    end = Offset(width, y30),
                    strokeWidth = 1f
                )

                // Draw shaded neutral zone between 30 and 70
                drawRect(
                    color = PurpleMA.copy(alpha = 0.04f),
                    topLeft = Offset(0f, y70),
                    size = Size(width, y30 - y70)
                )

                // RSI curve
                val rsiPath = Path()
                activeCandles.forEachIndexed { i, candle ->
                    val x = (i * slotWidth) + (slotWidth / 2)
                    val y = rsiToY(candle.rsi)
                    if (i == 0) rsiPath.moveTo(x, y) else rsiPath.lineTo(x, y)
                }
                drawPath(path = rsiPath, color = PurpleMA, style = Stroke(width = 2.2f))

                selectedIndex?.let { idx ->
                    val cx = (idx * slotWidth) + (slotWidth / 2)
                    drawLine(
                        color = TextPrimary.copy(alpha = 0.4f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, height),
                        strokeWidth = 1f
                    )
                }
            }
            if (activeIndicatorTab == "ALL") Spacer(modifier = Modifier.height(6.dp))
        }

        // SUB-PANE 3: MACD (12, 26, 9)
        if (activeIndicatorTab == "MACD" || activeIndicatorTab == "ALL") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MACD (12, 26, 9) TREND CONVERGENCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanRS
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val lastHist = currentInspected?.macdHist ?: 0f
                    val signalTag = if (lastHist >= 0) "BULLISH CROSSOVER" else "BEARISH MOMENTUM"
                    Text(
                        text = "• $signalTag",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastHist >= 0) BullishGreen else BearishRed
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("MACD (Cyan)", fontSize = 8.5.sp, color = CyanRS)
                    Text("Signal (Amber)", fontSize = 8.5.sp, color = AmberGold)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                val width = size.width
                val height = size.height
                val n = activeCandles.size
                if (n < 2) return@Canvas
                val slotWidth = width / n

                // Find max amplitude of MACD and Signal
                val maxAmp = maxOf(
                    activeCandles.maxOf { maxOf(abs(it.macd), abs(it.macdSignal), abs(it.macdHist)) },
                    1f
                )

                fun macdToY(v: Float): Float {
                    val halfH = height / 2f
                    return halfH - (v / maxAmp * (halfH * 0.85f))
                }

                // Zero baseline
                val zeroY = height / 2f
                drawLine(
                    color = DividerColor,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1f
                )

                // 1. Draw Histogram Bars
                val barW = (slotWidth * 0.45f).coerceAtLeast(3f)
                activeCandles.forEachIndexed { i, candle ->
                    val cx = (i * slotWidth) + (slotWidth / 2)
                    val histY = macdToY(candle.macdHist)
                    val isPositive = candle.macdHist >= 0
                    val top = if (isPositive) histY else zeroY
                    val barH = abs(histY - zeroY).coerceAtLeast(1.5f)
                    val barCol = if (isPositive) BullishGreen.copy(alpha = 0.65f) else BearishRed.copy(alpha = 0.65f)

                    drawRect(
                        color = barCol,
                        topLeft = Offset(cx - (barW / 2), top),
                        size = Size(barW, barH)
                    )
                }

                // 2. Draw MACD Line & Signal Line
                val macdPath = Path()
                val signalPath = Path()

                activeCandles.forEachIndexed { i, candle ->
                    val cx = (i * slotWidth) + (slotWidth / 2)
                    val my = macdToY(candle.macd)
                    val sy = macdToY(candle.macdSignal)

                    if (i == 0) {
                        macdPath.moveTo(cx, my)
                        signalPath.moveTo(cx, sy)
                    } else {
                        macdPath.lineTo(cx, my)
                        signalPath.lineTo(cx, sy)
                    }
                }

                drawPath(path = macdPath, color = CyanRS, style = Stroke(width = 2f))
                drawPath(path = signalPath, color = AmberGold, style = Stroke(width = 1.6f))

                selectedIndex?.let { idx ->
                    val cx = (idx * slotWidth) + (slotWidth / 2)
                    drawLine(
                        color = TextPrimary.copy(alpha = 0.4f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, height),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

/**
 * Computes missing RSI and MACD indicators on the fly from closes.
 */
private fun computeMissingIndicators(candles: List<CandleData>): List<CandleData> {
    val count = candles.size
    if (count == 0) return emptyList()

    val closes = candles.map { it.close }
    val rsiValues = FloatArray(count) { 50f }
    var avgGain = 0f
    var avgLoss = 0f
    val rsiPeriod = 14

    for (i in 1 until count) {
        val change = closes[i] - closes[i - 1]
        val gain = if (change > 0) change else 0f
        val loss = if (change < 0) -change else 0f
        if (i <= rsiPeriod) {
            avgGain += gain
            avgLoss += loss
            if (i == rsiPeriod) {
                avgGain /= rsiPeriod
                avgLoss /= rsiPeriod
            }
        } else {
            avgGain = (avgGain * (rsiPeriod - 1) + gain) / rsiPeriod
            avgLoss = (avgLoss * (rsiPeriod - 1) + loss) / rsiPeriod
        }
        if (i >= rsiPeriod) {
            val rs = if (avgLoss <= 0.0001f) 100f else avgGain / avgLoss
            rsiValues[i] = (100f - (100f / (1f + rs))).coerceIn(10f, 95f)
        } else {
            val earlyRs = if (avgLoss <= 0.0001f) 2f else (avgGain.coerceAtLeast(0.1f) / avgLoss.coerceAtLeast(0.1f))
            rsiValues[i] = (100f - (100f / (1f + earlyRs))).coerceIn(30f, 75f)
        }
    }
    rsiValues[0] = rsiValues.getOrElse(1) { 50f }

    val k12 = 2f / (12f + 1f)
    val k26 = 2f / (26f + 1f)
    val k9 = 2f / (9f + 1f)

    val ema12 = FloatArray(count) { closes[0] }
    val ema26 = FloatArray(count) { closes[0] }
    val macdLine = FloatArray(count) { 0f }
    val signalLine = FloatArray(count) { 0f }
    val histLine = FloatArray(count) { 0f }

    for (i in 1 until count) {
        ema12[i] = (closes[i] * k12) + (ema12[i - 1] * (1f - k12))
        ema26[i] = (closes[i] * k26) + (ema26[i - 1] * (1f - k26))
        macdLine[i] = ema12[i] - ema26[i]
    }
    signalLine[0] = macdLine[0]
    for (i in 1 until count) {
        signalLine[i] = (macdLine[i] * k9) + (signalLine[i - 1] * (1f - k9))
        histLine[i] = macdLine[i] - signalLine[i]
    }

    return candles.mapIndexed { idx, candle ->
        candle.copy(
            rsi = rsiValues[idx],
            macd = macdLine[idx],
            macdSignal = signalLine[idx],
            macdHist = histLine[idx]
        )
    }
}
