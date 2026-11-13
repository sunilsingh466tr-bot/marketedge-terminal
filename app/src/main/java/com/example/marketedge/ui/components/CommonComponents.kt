package com.example.marketedge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TerminalHeader(
    nifty50Level: Double = 25299.35,
    nifty50Change: Double = 44.30,
    nifty50ChangePercent: Double = 0.18,
    nifty500Level: Double = 23779.30,
    nifty500Change: Double = 212.40,
    nifty500ChangePercent: Double = 0.90,
    isDarkTheme: Boolean = false,
    isLiveStreaming: Boolean = false,
    streamTick: Int = 0,
    onToggleStream: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(BullishGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MARKETEDGE TERMINAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isLiveStreaming) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveStreaming) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clickable { onToggleStream() }
                            .testTag("stream_toggle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveStreaming) BullishGreen else BearishRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveStreaming) "1s LIVE #$streamTick" else "PAUSED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isLiveStreaming) BullishGreen else BearishRed
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NIFTY 50: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%,.2f", nifty50Level),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val isPositive = nifty50Change >= 0
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", nifty50Change)} (${if (isPositive) "+" else ""}${String.format("%.2f", nifty50ChangePercent)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPositive) BullishGreen else BearishRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• N500: ${String.format("%,.0f", nifty500Level)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Theme Toggle Icon Button
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .testTag("header_theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                        tint = AmberGold,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .testTag("header_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search stock",
                        tint = CyanRS,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .testTag("header_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh data",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SparklineCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true
) {
    if (points.size < 2) return

    val strokeColor = if (isPositive) BullishGreen else BearishRed
    val gradientColor = if (isPositive) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, value ->
            val x = (index.toFloat() / (points.size - 1)) * width
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * (height * 0.85f)) - (height * 0.075f)

            if (index == 0) {
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

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.2f)
        )
    }
}

@Composable
fun RangeBar52W(
    low: Double,
    high: Double,
    current: Double,
    modifier: Modifier = Modifier
) {
    val totalRange = (high - low).coerceAtLeast(0.01)
    val fraction = ((current - low) / totalRange).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "52W L: ₹${String.format("%,.1f", low)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Text(
                text = "Current: ₹${String.format("%,.1f", current)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
            Text(
                text = "52W H: ₹${String.format("%,.1f", high)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TerminalCardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(BearishRed.copy(alpha = 0.5f), AmberGold, BullishGreen)
                        )
                    )
            )
        }
    }
}

@Composable
fun RsScoreBadge(
    rsScore: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when {
        rsScore >= 90 -> BullishGreenDark to BullishGreenGlow
        rsScore >= 80 -> CyanRSDark to CyanRS
        rsScore >= 60 -> AmberWarning.copy(alpha = 0.3f) to AmberGold
        else -> BearishRedDark to BearishRedGlow
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "RS ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
            Text(
                text = "$rsScore",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}

@Composable
fun VolumeSurgeBadge(
    multiplier: Float,
    modifier: Modifier = Modifier
) {
    val isHeavy = multiplier >= 3.0f
    val (bgColor, tintColor) = if (isHeavy) {
        AmberGold.copy(alpha = 0.2f) to AmberGold
    } else {
        TerminalCardElevated to TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, tintColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${String.format("%.1f", multiplier)}x 20DMA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = tintColor
        )
    }
}
