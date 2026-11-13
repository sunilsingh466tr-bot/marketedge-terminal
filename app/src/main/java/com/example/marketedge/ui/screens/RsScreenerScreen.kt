package com.example.marketedge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.ScreenerStock
import com.example.marketedge.ui.ScreenerPreset
import com.example.marketedge.ui.ScreenerSort
import com.example.marketedge.ui.components.RangeBar52W
import com.example.marketedge.ui.components.RsScoreBadge
import com.example.marketedge.ui.components.VolumeSurgeBadge
import com.example.ui.theme.*

/**
 * Relative Strength (RS) Screener list view for Nifty 500 stocks.
 * Displays ticker symbols, current price, and calculated momentum score
 * along with alpha vs benchmark, technical moving average alignment, and pattern tags.
 */
@Composable
fun RsScreenerScreen(
    stocks: List<ScreenerStock>,
    activePreset: ScreenerPreset,
    activeSort: ScreenerSort,
    selectedSector: String,
    onPresetSelect: (ScreenerPreset) -> Unit,
    onSortSelect: (ScreenerSort) -> Unit,
    onSectorSelect: (String) -> Unit,
    onStockClick: (ScreenerStock) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var isTableView by remember { mutableStateOf(false) }
    var inlineSearchQuery by remember { mutableStateOf("") }
    var isMomentumInfoExpanded by remember { mutableStateOf(false) }

    val sectors = listOf(
        "All", "Electronics / EMS", "Retail", "Power T&D / Infra",
        "Defence", "IT Services", "Capital Goods", "Auto", "Banking",
        "Railways", "FMCG"
    )

    // Filter stocks based on inline search query
    val displayedStocks = remember(stocks, inlineSearchQuery) {
        if (inlineSearchQuery.isBlank()) stocks
        else stocks.filter {
            it.symbol.contains(inlineSearchQuery.trim(), ignoreCase = true) ||
            it.name.contains(inlineSearchQuery.trim(), ignoreCase = true) ||
            it.sector.contains(inlineSearchQuery.trim(), ignoreCase = true)
        }
    }

    // Key momentum summary metrics for the active group
    val superLeadersCount = remember(displayedStocks) { displayedStocks.count { it.rsScore >= 95 } }
    val avgRsScore = remember(displayedStocks) {
        if (displayedStocks.isNotEmpty()) displayedStocks.map { it.rsScore }.average().toInt() else 0
    }
    val avgAlpha = remember(displayedStocks) {
        if (displayedStocks.isNotEmpty()) {
            displayedStocks.map { it.threeMonthReturnPercent - it.nifty500ThreeMonthReturn }.average()
        } else 0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                // Header Row with Title, Momentum Info Toggle, and View Mode Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RELATIVE STRENGTH SCREENER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { isMomentumInfoExpanded = !isMomentumInfoExpanded },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("rs_momentum_info_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Momentum Info",
                                    tint = if (isMomentumInfoExpanded) CyanRS else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Nifty 500 Benchmark • 3-Month Rolling RS Momentum (0–99)",
                            fontSize = 11.sp,
                            color = CyanRS
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // View Toggle: Card vs Dense Table
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalCard)
                                .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            IconButton(
                                onClick = { isTableView = false },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isTableView) CyanRSDark else Color.Transparent)
                                    .testTag("view_mode_cards_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = "Card View",
                                    tint = if (!isTableView) CyanRS else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { isTableView = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTableView) CyanRSDark else Color.Transparent)
                                    .testTag("view_mode_table_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Table View",
                                    tint = if (isTableView) CyanRS else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Sort button with dropdown
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TerminalCard)
                                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("screener_sort_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Sort",
                                    tint = CyanRS,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sort",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(TerminalSurface)
                            ) {
                                ScreenerSort.values().forEach { sortOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sortOption.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (activeSort == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (activeSort == sortOption) CyanRS else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            onSortSelect(sortOption)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Momentum Calculation Methodology Card
                AnimatedVisibility(visible = isMomentumInfoExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TerminalCardElevated)
                            .border(1.dp, CyanRS.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = CyanRS,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CALCULATED RELATIVE STRENGTH (RS) METHODOLOGY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Relative Strength (RS) ranks each stock's 3-month performance against all 500 constituent stocks in the NIFTY 500 on a percentile scale (0 to 99).\n" +
                                   "• RS 95–99: Top 5% institutional momentum leaders in India. Outperforming both market pullbacks and peer sectors.\n" +
                                   "• RS 80–94: Strong Stage-2 bull advance with institutional accumulation.\n" +
                                   "• RS < 50: Lagging behind the broader market index.",
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Inline Search Bar for quick ticker/company lookup
                OutlinedTextField(
                    value = inlineSearchQuery,
                    onValueChange = { inlineSearchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search Ticker Symbol (e.g. DIXON, BEL, TRENT) or Company...",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyanRS,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (inlineSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { inlineSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface,
                        focusedBorderColor = CyanRS,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("screener_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Statistics Bar (Matches, Super Leaders, Avg Alpha)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Filtered", fontSize = 9.sp, color = TextTertiary)
                        Text(
                            text = "${displayedStocks.size} Stocks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(TerminalBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Super Leaders (RS≥95)", fontSize = 9.sp, color = TextTertiary)
                        Text(
                            text = "$superLeadersCount Stocks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = BullishGreen
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(TerminalBorder))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Avg 3M Alpha", fontSize = 9.sp, color = TextTertiary)
                        Text(
                            text = "+${String.format("%.1f", avgAlpha)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanRS
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Presets Horizontal Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(ScreenerPreset.values()) { preset ->
                        PresetChip(
                            label = preset.label,
                            isSelected = activePreset == preset,
                            onClick = { onPresetSelect(preset) },
                            testTag = "preset_${preset.name}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sectors Horizontal Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(sectors) { sector ->
                        SectorChip(
                            label = sector,
                            isSelected = selectedSector == sector,
                            onClick = { onSectorSelect(sector) },
                            testTag = "sector_${sector.replace(" ", "_")}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Match count & active sort indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Viewing ${displayedStocks.size} Nifty 500 stocks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text(
                        text = "Sort: ${activeSort.label}",
                        fontSize = 10.sp,
                        color = CyanRS
                    )
                }
            }
        }

        // Empty state when search or filter returns zero matches
        if (displayedStocks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = TextTertiary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching stocks found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query, sector, or preset filter.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                inlineSearchQuery = ""
                                onPresetSelect(ScreenerPreset.ALL)
                                onSectorSelect("All")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanRS)
                        ) {
                            Text("Reset All Filters", fontSize = 11.sp, color = CyanRS)
                        }
                    }
                }
            }
        } else if (isTableView) {
            // DENSE INSTITUTIONAL TABLE LIST VIEW
            item {
                TableHeaderRow()
            }
            items(displayedStocks, key = { it.symbol }) { stock ->
                ScreenerStockTableRow(
                    stock = stock,
                    onClick = { onStockClick(stock) }
                )
            }
        } else {
            // RICH MOMENTUM CARD VIEW
            items(displayedStocks, key = { it.symbol }) { stock ->
                ScreenerStockCard(
                    stock = stock,
                    onClick = { onStockClick(stock) }
                )
            }
        }
    }
}

/**
 * Rich Momentum Card for RS Screener displaying Ticker, Price,
 * Calculated Momentum Score gauge, Alpha, MA stack, and pattern tags.
 */
@Composable
private fun ScreenerStockCard(
    stock: ScreenerStock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = stock.changePercent >= 0
    val alphaOverNifty = stock.threeMonthReturnPercent - stock.nifty500ThreeMonthReturn
    val isSuperLeader = stock.rsScore >= 95

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(
                1.dp,
                if (isSuperLeader) BullishGreen.copy(alpha = 0.5f) else TerminalBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("stock_card_${stock.symbol}")
    ) {
        // Row 1: Ticker Symbol, Market Cap, Sector, and Calculated Momentum Score Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
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
                            text = stock.marketCapCategory,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    if (isSuperLeader) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BullishGreenDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TOP 5%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = BullishGreenGlow
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.name,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stock.sector,
                    fontSize = 10.sp,
                    color = CyanRS
                )
            }

            // Price and Calculated Momentum Score
            Column(horizontalAlignment = Alignment.End) {
                RsScoreBadge(rsScore = stock.rsScore)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${String.format("%,.2f", stock.currentPrice)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPositive) BullishGreen else BearishRed
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Visual Calculated Momentum Score Gauge (0 to 100 Meter)
        CalculatedMomentumGauge(
            rsScore = stock.rsScore,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Metrics Grid (3M Return vs Nifty, Alpha, Dist to 52W High, Volume Surge)
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
                Text("3M Return vs N500", fontSize = 9.sp, color = TextTertiary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+${String.format("%.1f", stock.threeMonthReturnPercent)}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BullishGreen
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "(vs +${String.format("%.1f", stock.nifty500ThreeMonthReturn)}%)",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Alpha vs N500", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "${if (alphaOverNifty >= 0) "+" else ""}${String.format("%.1f", alphaOverNifty)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (alphaOverNifty >= 0) CyanRS else BearishRed
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("52W High Gap", fontSize = 9.sp, color = TextTertiary)
                Text(
                    text = "${String.format("%.1f", stock.distanceFrom52WHighPercent)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (stock.isNear52WHigh) BullishGreen else TextPrimary
                )
            }

            VolumeSurgeBadge(multiplier = stock.volumeMultiplier)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: Moving Average Technical Alignment Pill
        val isFullBullishStack = stock.currentPrice > stock.ema21 && stock.ema21 > stock.sma50 && stock.sma50 > stock.sma200
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalCardElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isFullBullishStack) BullishGreen else AmberGold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFullBullishStack) "Full Bullish Stack: Price > 21EMA > 50SMA > 200SMA"
                           else "Consolidating: Price > 50 SMA (${String.format("%,.0f", stock.sma50)})",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFullBullishStack) BullishGreenGlow else TextSecondary
                )
            }

            Text(
                text = "21EMA: ${String.format("%,.0f", stock.ema21)}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 5: Pattern Scanner Tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stock.patterns.take(3).forEach { pattern ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalCardElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pattern,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (pattern.contains("Leader") || pattern.contains("ATH")) AmberGold else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Custom visual progress gauge showing the stock's calculated RS Momentum score
 * along a 0 to 100 spectrum with institutional milestone markers (50, 80, 95).
 */
@Composable
private fun CalculatedMomentumGauge(
    rsScore: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALCULATED MOMENTUM SCORE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TextTertiary
            )
            Text(
                text = "$rsScore / 99 Percentile",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when {
                    rsScore >= 95 -> BullishGreenGlow
                    rsScore >= 80 -> CyanRS
                    rsScore >= 60 -> AmberGold
                    else -> BearishRed
                }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val width = size.width
            val height = size.height
            val radius = height / 2

            // Track background
            drawRoundRect(
                color = Color(0xFF1E2836),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )

            // Progress bar with color gradient based on score
            val progressFraction = (rsScore / 99f).coerceIn(0f, 1f)
            val fillWidth = width * progressFraction

            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0D9488),
                    Color(0xFF00E5FF),
                    if (rsScore >= 90) Color(0xFF00E676) else Color(0xFF00E5FF)
                ),
                startX = 0f,
                endX = fillWidth.coerceAtLeast(1f)
            )

            drawRoundRect(
                brush = brush,
                size = androidx.compose.ui.geometry.Size(fillWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )

            // Milestone Marker Ticks: 50% (Median), 80% (Leader), 95% (Super Alpha)
            val mark50 = width * 0.50f
            val mark80 = width * 0.80f
            val mark95 = width * 0.95f

            drawLine(color = Color.White.copy(alpha = 0.4f), start = Offset(mark50, 0f), end = Offset(mark50, height), strokeWidth = 1.dp.toPx())
            drawLine(color = Color.White.copy(alpha = 0.4f), start = Offset(mark80, 0f), end = Offset(mark80, height), strokeWidth = 1.dp.toPx())
            drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(mark95, 0f), end = Offset(mark95, height), strokeWidth = 1.5.dp.toPx())
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Milestone Labels Below Gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0 (Weak)", fontSize = 8.sp, color = TextTertiary)
            Text("50 (Median)", fontSize = 8.sp, color = TextTertiary)
            Text("80 (Strong)", fontSize = 8.sp, color = CyanRS)
            Text("95+ (Super)", fontSize = 8.sp, color = BullishGreenGlow)
        }
    }
}

/**
 * Institutional Table Header Row for High-Density Screener view.
 */
@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalCardElevated)
            .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TICKER / CO", modifier = Modifier.weight(1.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
        Text("RS SCORE", modifier = Modifier.weight(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanRS)
        Text("PRICE (1D)", modifier = Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
        Text("3M ALPHA", modifier = Modifier.weight(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
        Text("VOL / SETUP", modifier = Modifier.weight(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
    }
}

/**
 * Compact high-density table row for institutional scanning across Nifty 500 stocks.
 */
@Composable
private fun ScreenerStockTableRow(
    stock: ScreenerStock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = stock.changePercent >= 0
    val alphaOverNifty = stock.threeMonthReturnPercent - stock.nifty500ThreeMonthReturn

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("stock_table_row_${stock.symbol}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ticker & Sector
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = stock.symbol,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Text(
                text = stock.name,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // RS Score
        Box(modifier = Modifier.weight(0.7f)) {
            RsScoreBadge(rsScore = stock.rsScore)
        }

        // Price & 1D Change
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "₹${String.format("%,.2f", stock.currentPrice)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Text(
                text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = if (isPositive) BullishGreen else BearishRed
            )
        }

        // 3M Alpha vs Nifty 500
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = "+${String.format("%.1f", stock.threeMonthReturnPercent)}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BullishGreen
            )
            Text(
                text = "α ${if (alphaOverNifty >= 0) "+" else ""}${String.format("%.1f", alphaOverNifty)}%",
                fontSize = 9.sp,
                color = CyanRS
            )
        }

        // Volume Multiplier & Primary Pattern
        Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
            VolumeSurgeBadge(multiplier = stock.volumeMultiplier)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stock.patterns.firstOrNull() ?: stock.marketCapCategory,
                fontSize = 8.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
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
            .testTag(testTag)
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

@Composable
private fun SectorChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) BullishGreenDark else TerminalCard)
            .border(
                1.dp,
                if (isSelected) BullishGreen else TerminalBorder,
                RoundedCornerShape(6.dp)
            )
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BullishGreenGlow else TextSecondary
        )
    }
}
