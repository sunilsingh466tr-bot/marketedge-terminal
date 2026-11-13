package com.example.marketedge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.FiiDiiPoint
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun FiiDiiFlowChart(
    flows: List<FiiDiiPoint>,
    modifier: Modifier = Modifier
) {
    if (flows.isEmpty()) return

    // Show recent 10 sessions chronologically (oldest to newest)
    val displayFlows = remember(flows) { flows.take(10).reversed() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val totalDiiBuy = displayFlows.sumOf { it.diiNetCr }
    val totalFiiSell = displayFlows.sumOf { it.fiiNetCr }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FII & DII CASH FLOW (₹ Cr)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Last 10 Trading Sessions (Cash Market)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(BullishGreen, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DII", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.size(8.dp).background(BearishRed, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("FII", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected point inspector banner
        val inspectedPoint = selectedIndex?.let { displayFlows.getOrNull(it) } ?: displayFlows.lastOrNull()
        if (inspectedPoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalCard)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inspectedPoint.date,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
                Row {
                    Text(
                        text = "FII: ${if (inspectedPoint.fiiNetCr >= 0) "+" else ""}${String.format("%,.0f", inspectedPoint.fiiNetCr)} Cr  ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (inspectedPoint.fiiNetCr >= 0) BullishGreen else BearishRed
                    )
                    Text(
                        text = "DII: +${String.format("%,.0f", inspectedPoint.diiNetCr)} Cr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = BullishGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas bar chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(displayFlows) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / displayFlows.size
                        val index = (offset.x / barWidth).toInt().coerceIn(0, displayFlows.size - 1)
                        selectedIndex = index
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val slotWidth = width / displayFlows.size
            val maxCr = 5000.0 // Max scaling value for Crores

            // Zero line in the middle
            val zeroY = height * 0.55f

            // Draw zero line
            drawLine(
                color = DividerColor,
                start = Offset(0f, zeroY),
                end = Offset(width, zeroY),
                strokeWidth = 1.5f
            )

            displayFlows.forEachIndexed { i, pt ->
                val centerX = (i * slotWidth) + (slotWidth / 2)
                val barW = (slotWidth * 0.35f).coerceAtLeast(6f)

                // FII Bar (Can be positive or negative)
                val fiiHeight = ((abs(pt.fiiNetCr) / maxCr).toFloat() * (height * 0.42f)).coerceAtLeast(3f)
                val fiiTop = if (pt.fiiNetCr >= 0) zeroY - fiiHeight else zeroY
                val fiiColor = if (pt.fiiNetCr >= 0) BullishGreen else BearishRed

                drawRect(
                    color = fiiColor,
                    topLeft = Offset(centerX - barW - 1f, fiiTop),
                    size = Size(barW, fiiHeight)
                )

                // DII Bar (Mostly positive buy)
                val diiHeight = ((abs(pt.diiNetCr) / maxCr).toFloat() * (height * 0.42f)).coerceAtLeast(3f)
                val diiTop = if (pt.diiNetCr >= 0) zeroY - diiHeight else zeroY
                val diiColor = if (pt.diiNetCr >= 0) BullishGreen else BearishRed

                drawRect(
                    color = diiColor,
                    topLeft = Offset(centerX + 1f, diiTop),
                    size = Size(barW, diiHeight)
                )

                // Highlight selected column
                if (selectedIndex == i) {
                    drawRect(
                        color = CyanRS.copy(alpha = 0.15f),
                        topLeft = Offset(i * slotWidth, 0f),
                        size = Size(slotWidth, height)
                    )
                }
            }
        }

        // Dates Row below chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayFlows.forEachIndexed { i, pt ->
                if (i % 2 == 0 || i == displayFlows.size - 1) {
                    Text(
                        text = pt.date,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rolling 10-day summary pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCardElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-Day Rolling Net:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Text(
                text = "DII Net: +₹${String.format("%,.0f", totalDiiBuy)} Cr | FII Net: ${if (totalFiiSell >= 0) "+" else ""}${String.format("%,.0f", totalFiiSell)} Cr",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
        }
    }
}
