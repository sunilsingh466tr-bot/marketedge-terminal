package com.example.marketedge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.marketedge.data.MarketPulseData
import com.example.marketedge.model.*
import com.example.marketedge.ui.components.FiiDiiFlowChart
import com.example.marketedge.ui.components.InstitutionalFlows
import com.example.ui.theme.*

@Composable
fun MarketPulseScreen(
    pulseData: MarketPulseData,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBulletTag by remember { mutableStateOf<MarketTag?>(null) }

    val filteredBullets = remember(pulseData.bullets, selectedBulletTag) {
        if (selectedBulletTag == null) pulseData.bullets
        else pulseData.bullets.filter { it.tag == selectedBulletTag }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            // DESKTOP / TABLET TWO-COLUMN LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Institutional Flows & FII/DII Chart
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MarketStatusBanner(pulseData = pulseData)
                    }
                    item {
                        InstitutionalFlows(flows = pulseData.fiiDiiFlows)
                    }
                    item {
                        FiiDiiFlowChart(flows = pulseData.fiiDiiFlows)
                    }
                }

                // Right Column: Market Breadth & Market Bullets
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MarketBreadthSection(breadth = pulseData.breadth)
                    }
                    item {
                        BulletsHeader(
                            totalCount = filteredBullets.size,
                            bullets = pulseData.bullets,
                            selectedTag = selectedBulletTag,
                            onTagSelect = { selectedBulletTag = it }
                        )
                    }
                    items(filteredBullets, key = { it.id }) { bullet ->
                        BulletItemCard(bullet = bullet)
                    }
                }
            }
        } else {
            // MOBILE / COMPACT VERTICAL LAYOUT
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    MarketStatusBanner(pulseData = pulseData)
                }

                // InstitutionalFlows Composable with Trend Indicators & KPI Cards
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InstitutionalFlows(flows = pulseData.fiiDiiFlows)
                    }
                }

                // Section 1: FII / DII Historical Bar Graph (Net Buy/Sell in ₹ Cr)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FiiDiiFlowChart(flows = pulseData.fiiDiiFlows)
                    }
                }

                // Section 2: Market Breadth & Participation Dashboard
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MarketBreadthSection(breadth = pulseData.breadth)
                    }
                }

                // Section 3: Daily Market Pulse (10-15 Bullets)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BulletsHeader(
                            totalCount = filteredBullets.size,
                            bullets = pulseData.bullets,
                            selectedTag = selectedBulletTag,
                            onTagSelect = { selectedBulletTag = it }
                        )
                    }
                }

                // List of Bullets
                items(filteredBullets, key = { it.id }) { bullet ->
                    BulletItemCard(
                        bullet = bullet,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketStatusBanner(pulseData: MarketPulseData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BullishGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pulseData.asOfDate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NIFTY 50: ${String.format("%,.0f", pulseData.nifty50Level)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (pulseData.nifty50Change >= 0) BullishGreen else BearishRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RS Ref: NIFTY 500",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }
        }
    }
}

@Composable
private fun BulletsHeader(
    totalCount: Int,
    bullets: List<MarketBullet>,
    selectedTag: MarketTag?,
    onTagSelect: (MarketTag?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TODAY'S MARKET PULSE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Text(
                    text = "$totalCount Key Institutional Takeaways & Announcements",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bullet Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChipPill(
                    label = "All (${bullets.size})",
                    isSelected = selectedTag == null,
                    onClick = { onTagSelect(null) },
                    tag = "bullet_filter_all"
                )
            }
            items(MarketTag.values()) { tag ->
                val count = bullets.count { it.tag == tag }
                FilterChipPill(
                    label = "${tag.displayName} ($count)",
                    isSelected = selectedTag == tag,
                    onClick = { onTagSelect(tag) },
                    tag = "bullet_filter_${tag.name}"
                )
            }
        }
    }
}

@Composable
private fun MarketBreadthSection(
    breadth: MarketBreadth,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = "NSE MARKET BREADTH & INTERNALS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "A/D Ratio: ${String.format("%.2f", breadth.advances.toFloat() / breadth.declines)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Advance / Decline Split Visual Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${breadth.advances} Advances (${String.format("%.0f", breadth.advancePercent)}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
            Text(
                text = "${breadth.declines} Declines",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BearishRed
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BearishRedDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(breadth.advancePercent / 100f)
                    .fillMaxHeight()
                    .background(BullishGreen)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Momentum / Performance Buckets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MomentumMetricCard(
                title = "Upper Circuit",
                value = "${breadth.upperCircuit20}",
                color = BullishGreen,
                modifier = Modifier.weight(1f)
            )
            MomentumMetricCard(
                title = "Lower Circuit",
                value = "${breadth.lowerCircuit}",
                color = BearishRed,
                modifier = Modifier.weight(1f)
            )
            MomentumMetricCard(
                title = "52W Highs",
                value = "${breadth.highs52W}",
                color = CyanRS,
                modifier = Modifier.weight(1f)
            )
            MomentumMetricCard(
                title = "52W Lows",
                value = "${breadth.lows52W}",
                color = BearishRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Official Exchange Telemetry Row (From NSE / BSE Statistics)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL MARKET CAP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
                Text(
                    text = breadth.marketCap,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = BullishGreen
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "REG CLIENT ACCOUNTS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
                Text(
                    text = "${breadth.registeredAccounts} • ${breadth.latencyNs}ns",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Market-Cap Sliced Breadth
        Text(
            text = "Market-Cap Segmented Breadth (% Above 50 EMA)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        CapSliceRow(cap = breadth.largeCapBreadth)
        Spacer(modifier = Modifier.height(6.dp))
        CapSliceRow(cap = breadth.midCapBreadth)
        Spacer(modifier = Modifier.height(6.dp))
        CapSliceRow(cap = breadth.smallCapBreadth)
    }
}

@Composable
private fun CapSliceRow(cap: CapBreadth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalCard)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = cap.category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${cap.advances} Adv / ${cap.declines} Dec",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TerminalCardElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cap.percentAbove50Ema / 100f)
                        .fillMaxHeight()
                        .background(if (cap.percentAbove50Ema >= 70) BullishGreen else AmberGold)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${cap.percentAbove50Ema}% >50E",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (cap.percentAbove50Ema >= 70) BullishGreen else AmberGold
            )
        }
    }
}

@Composable
private fun MomentumMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun BulletItemCard(
    bullet: MarketBullet,
    modifier: Modifier = Modifier
) {
    val impactColor = when (bullet.impact) {
        MarketImpact.BULLISH -> BullishGreen
        MarketImpact.BEARISH -> BearishRed
        MarketImpact.NEUTRAL -> TextSecondary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TerminalCardElevated)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = bullet.tag.displayName.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = CyanRS
                )
            }

            // Impact badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(impactColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = bullet.impact.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = impactColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = bullet.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = bullet.description,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyanRSDark else TerminalCard)
            .border(
                1.dp,
                if (isSelected) CyanRS else TerminalBorder,
                RoundedCornerShape(20.dp)
            )
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CyanRS else TextSecondary
        )
    }
}
