package com.example.marketedge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.FiiDiiPoint
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * InstitutionalFlows Composable
 * Displays a summary of daily FII and DII buying/selling values,
 * styled as a card-based view with trend indicators.
 */
@Composable
fun InstitutionalFlows(
    flows: List<FiiDiiPoint>,
    modifier: Modifier = Modifier
) {
    if (flows.isEmpty()) return

    var selectedWindowDays by remember { mutableStateOf(10) }
    var isExpandedList by remember { mutableStateOf(false) }

    val activeFlows = remember(flows, selectedWindowDays) {
        flows.take(selectedWindowDays)
    }

    val latestFlow = flows.firstOrNull() ?: flows[0]
    val previousFlow = flows.getOrNull(1)

    // Calculate rolling statistics
    val totalDiiBuy = activeFlows.sumOf { it.diiNetCr }
    val totalFiiSell = activeFlows.sumOf { it.fiiNetCr }
    val netTotal = totalDiiBuy + totalFiiSell

    // Streak calculation for DII
    val diiStreak = remember(flows) {
        var streak = 0
        for (f in flows) {
            if (f.diiNetCr > 0) streak++ else break
        }
        streak
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("institutional_flows_card")
    ) {
        // Section Title & Trend Window Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyanRS)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INSTITUTIONAL FLOWS (FII & DII)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Daily Cash Market Activity & Cumulative Bias",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Window Selection Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(5, 10, 14).forEach { days ->
                    val isSelected = selectedWindowDays == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanRSDark else TerminalCard)
                            .border(1.dp, if (isSelected) CyanRS else TerminalBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedWindowDays = days }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${days}D",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyanRS else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card-Based View: Summary KPI Row for Today / Latest Session
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // FII Card
            val isFiiBuying = latestFlow.fiiNetCr >= 0
            val fiiTrendChange = if (previousFlow != null) latestFlow.fiiNetCr - previousFlow.fiiNetCr else 0.0
            FlowKpiCard(
                institutionName = "FII Net Cash",
                amountCr = latestFlow.fiiNetCr,
                isBuying = isFiiBuying,
                statusTag = if (isFiiBuying) "Net Buyer" else "Net Seller",
                trendSubtext = if (fiiTrendChange > 0) "▲ Outflow Easing" else "▼ Selling Widening",
                trendPositive = fiiTrendChange > 0,
                modifier = Modifier.weight(1f),
                testTag = "fii_kpi_card"
            )

            // DII Card
            val isDiiBuying = latestFlow.diiNetCr >= 0
            FlowKpiCard(
                institutionName = "DII Net Cash",
                amountCr = latestFlow.diiNetCr,
                isBuying = isDiiBuying,
                statusTag = if (isDiiBuying) "Net Buyer" else "Net Seller",
                trendSubtext = "▲ $diiStreak-Day Buying Streak",
                trendPositive = true,
                modifier = Modifier.weight(1f),
                testTag = "dii_kpi_card"
            )

            // Combined Net Impact Card
            val isCombinedNetPositive = latestFlow.totalNetCr >= 0
            FlowKpiCard(
                institutionName = "Net Institutional",
                amountCr = latestFlow.totalNetCr,
                isBuying = isCombinedNetPositive,
                statusTag = if (isCombinedNetPositive) "Absorption" else "Net Selling",
                trendSubtext = if (isCombinedNetPositive) "▲ DII Absorption" else "▼ Outflow Drag",
                trendPositive = isCombinedNetPositive,
                modifier = Modifier.weight(1.1f),
                testTag = "net_impact_kpi_card"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rolling Cumulative Bias Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${selectedWindowDays}-Day Rolling Net:",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${if (netTotal >= 0) "+₹" else "-₹"}${String.format("%,.0f", abs(netTotal))} Cr Net Domestic Absorption",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netTotal >= 0) BullishGreen else BearishRed
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (totalDiiBuy >= abs(totalFiiSell)) BullishGreenDark else BearishRedDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (totalDiiBuy >= abs(totalFiiSell)) "DII Dominated (Absorbing FII)" else "FII Pressure High",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDiiBuy >= abs(totalFiiSell)) BullishGreen else BearishRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Card-Based Horizontal Feed
        Text(
            text = "Recent Daily Breakdown (Cards with Trend Indicators):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(activeFlows) { flow ->
                DailyFlowSessionCard(flow = flow)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Expandable Detailed Table Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpandedList = !isExpandedList }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpandedList) "Hide Historical Matrix ▲" else "View All Historical Sessions ▼",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanRS
            )
        }

        AnimatedVisibility(visible = isExpandedList) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                flows.forEach { item ->
                    HistoricalFlowRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun FlowKpiCard(
    institutionName: String,
    amountCr: Double,
    isBuying: Boolean,
    statusTag: String,
    trendSubtext: String,
    trendPositive: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val valueColor = if (isBuying) BullishGreen else BearishRed
    val bgPillColor = if (isBuying) BullishGreenDark else BearishRedDark

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = institutionName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                maxLines = 1
            )
            Icon(
                imageVector = if (isBuying) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${if (amountCr >= 0) "+₹" else "-₹"}${String.format("%,.0f", abs(amountCr))}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )

        Text(
            text = "Cr",
            fontSize = 9.sp,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bgPillColor)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = statusTag,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = trendSubtext,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (trendPositive) BullishGreen else BearishRed,
            maxLines = 1
        )
    }
}

@Composable
private fun DailyFlowSessionCard(
    flow: FiiDiiPoint,
    modifier: Modifier = Modifier
) {
    val isNetPositive = flow.totalNetCr >= 0

    Column(
        modifier = modifier
            .width(135.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = flow.date,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isNetPositive) BullishGreenDark else BearishRedDark)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isNetPositive) "▲ NET BUY" else "▼ NET SELL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNetPositive) BullishGreen else BearishRed
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // FII Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("FII:", fontSize = 10.sp, color = TextSecondary)
            Text(
                text = "${if (flow.fiiNetCr >= 0) "+" else ""}${String.format("%,.0f", flow.fiiNetCr)} Cr",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (flow.fiiNetCr >= 0) BullishGreen else BearishRed
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // DII Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DII:", fontSize = 10.sp, color = TextSecondary)
            Text(
                text = "+${String.format("%,.0f", flow.diiNetCr)} Cr",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Net Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalCardElevated)
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Net: ${if (flow.totalNetCr >= 0) "+" else ""}${String.format("%,.0f", flow.totalNetCr)} Cr",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isNetPositive) BullishGreen else BearishRed
            )
        }
    }
}

@Composable
private fun HistoricalFlowRow(item: FiiDiiPoint) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalCard)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.date,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FII: ${if (item.fiiNetCr >= 0) "+" else ""}${String.format("%,.0f", item.fiiNetCr)} Cr",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (item.fiiNetCr >= 0) BullishGreen else BearishRed
            )
            Text(
                text = "DII: +${String.format("%,.0f", item.diiNetCr)} Cr",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
            Text(
                text = "Net: ${if (item.totalNetCr >= 0) "+" else ""}${String.format("%,.0f", item.totalNetCr)} Cr",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (item.totalNetCr >= 0) BullishGreen else BearishRed
            )
        }
    }
}
