package com.example.marketedge.ui.components

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
import com.example.marketedge.model.UpcomingEarnings
import com.example.marketedge.model.VolatilityExpectation
import com.example.ui.theme.*

/**
 * EarningsRadar Composable
 * Displays a list of upcoming earnings announcements for the current week,
 * including company names, expected dates, and an indicator for volatility expectations.
 */
@Composable
fun EarningsRadar(
    upcomingEarnings: List<UpcomingEarnings>,
    onStockClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (upcomingEarnings.isEmpty()) return

    var selectedDayFilter by remember { mutableStateOf("All") }
    var selectedVolatilityFilter by remember { mutableStateOf<VolatilityExpectation?>(null) }

    val daysOfWeek = remember(upcomingEarnings) {
        listOf("All") + upcomingEarnings.map { it.dayOfWeek }.distinct()
    }

    val filteredList = remember(upcomingEarnings, selectedDayFilter, selectedVolatilityFilter) {
        upcomingEarnings.filter { item ->
            val matchesDay = selectedDayFilter == "All" || item.dayOfWeek == selectedDayFilter
            val matchesVol = selectedVolatilityFilter == null || item.volatility == selectedVolatilityFilter
            matchesDay && matchesVol
        }
    }

    val highVolCount = upcomingEarnings.count { it.volatility == VolatilityExpectation.HIGH }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("earnings_radar_component")
    ) {
        // Header
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
                            .background(AmberGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EARNINGS RADAR (THIS WEEK)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Upcoming Corporate Results & Implied Volatility Expectations",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanRSDark)
                    .border(1.dp, CyanRS.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${upcomingEarnings.size} Releases",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Volatility Alert Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmberWarning.copy(alpha = 0.12f))
                .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$highVolCount High-Volatility Announcements Ahead",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Options straddles price sharp post-result swings. Expect high institutional volume.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter 1: Days of Week Filter Chips
        Text(
            text = "Filter by Release Day:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(daysOfWeek) { day ->
                val isSelected = selectedDayFilter == day
                val count = if (day == "All") upcomingEarnings.size else upcomingEarnings.count { it.dayOfWeek == day }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CyanRSDark else TerminalCard)
                        .border(1.dp, if (isSelected) CyanRS else TerminalBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedDayFilter = day }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("radar_day_$day")
                ) {
                    Text(
                        text = "$day ($count)",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) CyanRS else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter 2: Volatility Expectation Filter Chips
        Text(
            text = "Filter by Volatility Expectation:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VolatilityFilterPill(
                label = "All",
                isSelected = selectedVolatilityFilter == null,
                onClick = { selectedVolatilityFilter = null },
                modifier = Modifier.weight(1f)
            )
            VolatilityFilterPill(
                label = "High Vol",
                isSelected = selectedVolatilityFilter == VolatilityExpectation.HIGH,
                activeColor = BearishRed,
                onClick = { selectedVolatilityFilter = VolatilityExpectation.HIGH },
                modifier = Modifier.weight(1.1f)
            )
            VolatilityFilterPill(
                label = "Medium Vol",
                isSelected = selectedVolatilityFilter == VolatilityExpectation.MEDIUM,
                activeColor = AmberGold,
                onClick = { selectedVolatilityFilter = VolatilityExpectation.MEDIUM },
                modifier = Modifier.weight(1.2f)
            )
            VolatilityFilterPill(
                label = "Low Vol",
                isSelected = selectedVolatilityFilter == VolatilityExpectation.LOW,
                activeColor = BullishGreen,
                onClick = { selectedVolatilityFilter = VolatilityExpectation.LOW },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Upcoming Earnings Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredList.forEach { announcement ->
                UpcomingAnnouncementCard(
                    item = announcement,
                    onClick = { onStockClick(announcement.symbol) }
                )
            }
        }
    }
}

@Composable
private fun VolatilityFilterPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color = CyanRS,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.18f) else TerminalCard)
            .border(1.dp, if (isSelected) activeColor else TerminalBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else TextSecondary
        )
    }
}

@Composable
private fun UpcomingAnnouncementCard(
    item: UpcomingEarnings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("upcoming_card_${item.symbol}")
    ) {
        // Top Header: Company name, symbol, expected date badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.symbol,
                        fontSize = 13.sp,
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
                            text = item.marketCapCategory,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.companyName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = item.sector,
                    fontSize = 9.sp,
                    color = CyanRS
                )
            }

            // Expected Announcement Date & Slot Pill
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalCardElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = CyanRS,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.expectedDate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.timeSlot,
                    fontSize = 9.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Volatility Expectation Indicator Row
        VolatilityExpectationIndicator(
            volatility = item.volatility,
            impliedMove = item.impliedMovePercent,
            historicalReaction = item.historicalReactionPercent
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Consensus Estimates Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Est. Revenue", fontSize = 8.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", item.estimatedRevenueCr)} Cr",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Est. Net Profit (PAT)", fontSize = 8.sp, color = TextTertiary)
                Text(
                    text = "₹${String.format("%,.0f", item.estimatedPatCr)} Cr",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Historical Reaction", fontSize = 8.sp, color = TextTertiary)
                Text(
                    text = "±${String.format("%.1f", item.historicalReactionPercent)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Key Institutional Focus / Catalyst Area
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "Key Watch: ",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AmberGold
            )
            Text(
                text = item.keyFocus,
                fontSize = 9.sp,
                color = TextSecondary,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Visual Indicator for Volatility Expectations
 */
@Composable
fun VolatilityExpectationIndicator(
    volatility: VolatilityExpectation,
    impliedMove: Double,
    historicalReaction: Double,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeBorder, badgeText, badgeIcon) = when (volatility) {
        VolatilityExpectation.HIGH -> Quadruple(
            BearishRedDark,
            BearishRed.copy(alpha = 0.5f),
            BearishRed,
            Icons.Default.PriorityHigh
        )
        VolatilityExpectation.MEDIUM -> Quadruple(
            AmberGold.copy(alpha = 0.15f),
            AmberGold.copy(alpha = 0.5f),
            AmberGold,
            Icons.Default.ShowChart
        )
        VolatilityExpectation.LOW -> Quadruple(
            BullishGreenDark,
            BullishGreen.copy(alpha = 0.5f),
            BullishGreen,
            Icons.Default.Remove
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = volatility.label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = badgeText
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 3-pip intensity gauge
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { index ->
                    val isFilled = index < volatility.level
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) badgeText else badgeText.copy(alpha = 0.25f))
                    )
                }
            }
        }

        // Implied Move Metric
        Text(
            text = "Implied Move: ±${String.format("%.1f", impliedMove)}%",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = badgeText
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
