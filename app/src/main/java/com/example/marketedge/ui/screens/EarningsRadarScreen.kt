package com.example.marketedge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.EarningsResult
import com.example.marketedge.model.EarningsVerdict
import com.example.marketedge.model.UpcomingEarnings
import com.example.marketedge.ui.components.EarningsRadar
import com.example.marketedge.ui.components.VolumeSurgeBadge
import com.example.ui.theme.*

enum class EarningsViewTab(val label: String) {
    UPCOMING("Upcoming This Week"),
    PUBLISHED("Published Results"),
    COMBINED("Combined Radar")
}

@Composable
fun EarningsRadarScreen(
    upcomingEarnings: List<UpcomingEarnings>,
    results: List<EarningsResult>,
    activeFilter: String,
    onFilterSelect: (String) -> Unit,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedViewTab by remember { mutableStateOf(EarningsViewTab.UPCOMING) }
    val filterTabs = listOf("All", "Strong Beat", "In-Line", "Weak Miss", "Upper Circuit")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        val isDesktop = maxWidth >= 840.dp

        if (isDesktop) {
            // DESKTOP TWO-COLUMN LAYOUT: Upcoming Radar on Left, Published Results on Right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Upcoming Earnings Radar Composable
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        EarningsRadar(
                            upcomingEarnings = upcomingEarnings,
                            onStockClick = onStockClick
                        )
                    }
                }

                // Right Column: Published Results Matrix & Verdicts
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "PUBLISHED RESULTS & VOLUME SURGES",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${results.size} Companies Evaluated with Automated Verdicts",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PublishedFilterRow(
                                filterTabs = filterTabs,
                                activeFilter = activeFilter,
                                onFilterSelect = onFilterSelect
                            )
                        }
                    }

                    items(results, key = { it.symbol }) { result ->
                        EarningsResultCard(
                            result = result,
                            onClick = { onStockClick(result.symbol) }
                        )
                    }
                }
            }
        } else {
            // MOBILE COMPACT LAYOUT with View Mode Selector
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "EARNINGS & VOLUME RADAR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Weekly Announcements Calendar, Volatility & Results Reactions",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // View Mode Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EarningsViewTab.values().forEach { tab ->
                                val isSelected = selectedViewTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyanRSDark else TerminalCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyanRS else TerminalBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedViewTab = tab }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyanRS else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Content Based on Mode
                when (selectedViewTab) {
                    EarningsViewTab.UPCOMING -> {
                        item {
                            EarningsRadar(
                                upcomingEarnings = upcomingEarnings,
                                onStockClick = onStockClick
                            )
                        }
                    }

                    EarningsViewTab.PUBLISHED -> {
                        item {
                            PublishedFilterRow(
                                filterTabs = filterTabs,
                                activeFilter = activeFilter,
                                onFilterSelect = onFilterSelect
                            )
                        }

                        items(results, key = { it.symbol }) { result ->
                            EarningsResultCard(
                                result = result,
                                onClick = { onStockClick(result.symbol) }
                            )
                        }
                    }

                    EarningsViewTab.COMBINED -> {
                        item {
                            EarningsRadar(
                                upcomingEarnings = upcomingEarnings,
                                onStockClick = onStockClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "PUBLISHED RESULTS & PRICE REACTIONS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PublishedFilterRow(
                                filterTabs = filterTabs,
                                activeFilter = activeFilter,
                                onFilterSelect = onFilterSelect
                            )
                        }

                        items(results, key = { it.symbol }) { result ->
                            EarningsResultCard(
                                result = result,
                                onClick = { onStockClick(result.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishedFilterRow(
    filterTabs: List<String>,
    activeFilter: String,
    onFilterSelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(filterTabs) { tab ->
            val isSelected = activeFilter == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) CyanRSDark else TerminalCard)
                    .border(
                        1.dp,
                        if (isSelected) CyanRS else TerminalBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("earnings_filter_${tab.replace(" ", "_")}")
                    .clickable { onFilterSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tab,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CyanRS else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EarningsResultCard(
    result: EarningsResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = result.priceReactionPercent >= 0

    val (verdictBg, verdictColor) = when (result.verdict) {
        EarningsVerdict.STRONG_BEAT -> BullishGreenDark to BullishGreenGlow
        EarningsVerdict.IN_LINE -> AmberWarning.copy(alpha = 0.3f) to AmberGold
        EarningsVerdict.WEAK_MISS -> BearishRedDark to BearishRedGlow
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("earnings_card_${result.symbol}")
    ) {
        // Row 1: Symbol, Quarter, Verdict, Reaction %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalCardElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = result.quarter,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanRS
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.companyName,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = result.sector,
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Verdict Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(verdictBg)
                        .border(1.dp, verdictColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = result.verdict.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = verdictColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price reaction
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reaction: ",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.1f", result.priceReactionPercent)}%${if (result.isUpperCircuit) " (UC)" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPositive) BullishGreen else BearishRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Financial Matrix (Revenue & PAT YoY/QoQ)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Revenue (Topline)", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", result.revenueCr)} Cr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "YoY ${if (result.revenueYoy >= 0) "+" else ""}${String.format("%.1f", result.revenueYoy)}% | QoQ ${if (result.revenueQoq >= 0) "+" else ""}${String.format("%.1f", result.revenueQoq)}%",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (result.revenueYoy >= 0) BullishGreen else BearishRed
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PAT (Net Profit)", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", result.patCr)} Cr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "YoY ${if (result.patYoy >= 0) "+" else ""}${String.format("%.1f", result.patYoy)}% | QoQ ${if (result.patQoq >= 0) "+" else ""}${String.format("%.1f", result.patQoq)}%",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (result.patYoy >= 0) BullishGreen else BearishRed
                )
            }

            VolumeSurgeBadge(multiplier = result.volumeMultiplier)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Automated Market Behavior & Price Action Remark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalCardElevated)
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = "Automated Institutional Remark:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = CyanRS
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.marketRemark,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
