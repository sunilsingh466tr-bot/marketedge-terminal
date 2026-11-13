package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.marketedge.model.IndexCategory
import com.example.marketedge.ui.*
import com.example.marketedge.ui.components.GlobalSearchDialog
import com.example.marketedge.ui.components.TerminalHeader
import com.example.marketedge.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MarketEdgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MarketEdgeTheme(darkTheme = isDarkTheme) {
                MainTerminalApp(viewModel = viewModel, isDarkTheme = isDarkTheme)
            }
        }
    }
}

@Composable
fun MainTerminalApp(
    viewModel: MarketEdgeViewModel,
    isDarkTheme: Boolean
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedStock by viewModel.selectedStock.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allStocks by viewModel.allStocks.collectAsStateWithLifecycle()

    val pulseData by viewModel.pulseData.collectAsStateWithLifecycle()
    val indices by viewModel.indices.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val screenerStocks by viewModel.filteredScreenerStocks.collectAsStateWithLifecycle()
    val activePreset by viewModel.activePreset.collectAsStateWithLifecycle()
    val activeSort by viewModel.activeSort.collectAsStateWithLifecycle()
    val selectedSector by viewModel.selectedSector.collectAsStateWithLifecycle()

    val earningsResults by viewModel.filteredEarningsResults.collectAsStateWithLifecycle()
    val upcomingEarnings by viewModel.upcomingEarnings.collectAsStateWithLifecycle()
    val earningsFilter by viewModel.earningsFilter.collectAsStateWithLifecycle()

    val savedTheses by viewModel.savedTheses.collectAsStateWithLifecycle()
    val activeStockThesis by viewModel.activeStockThesis.collectAsStateWithLifecycle()
    val isLiveStreaming by viewModel.isLiveStreaming.collectAsStateWithLifecycle()
    val streamTick by viewModel.streamTick.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 760.dp

        if (isDesktop) {
            // DESKTOP & TABLET WIDE-SCREEN LAYOUT (NavigationRail on Left)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TerminalBackground)
            ) {
                // Left Navigation Rail
                TerminalNavigationRail(
                    currentTab = currentTab,
                    isDarkTheme = isDarkTheme,
                    onTabSelected = { viewModel.selectTab(it) },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onSearchClick = { viewModel.setSearchOpen(true) },
                    modifier = Modifier.fillMaxHeight()
                )

                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TerminalHeader(
                        nifty50Level = pulseData.nifty50Level,
                        nifty50Change = pulseData.nifty50Change,
                        nifty50ChangePercent = pulseData.nifty50ChangePercent,
                        nifty500Level = pulseData.nifty500Level,
                        nifty500Change = pulseData.nifty500Change,
                        nifty500ChangePercent = pulseData.nifty500ChangePercent,
                        isDarkTheme = isDarkTheme,
                        isLiveStreaming = isLiveStreaming,
                        streamTick = streamTick,
                        onToggleStream = { viewModel.toggleStreaming() },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onSearchClick = { viewModel.setSearchOpen(true) },
                        onRefreshClick = { viewModel.refreshAll() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (currentTab) {
                            TerminalTab.PULSE -> {
                                MarketPulseScreen(
                                    pulseData = pulseData,
                                    onStockClick = { symbol -> viewModel.openStockBySymbol(symbol) }
                                )
                            }

                            TerminalTab.INDICES -> {
                                IndexMatrixScreen(
                                    indices = indices,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategoryFilter(it) }
                                )
                            }

                            TerminalTab.SCREENER -> {
                                // Desktop Split Layout: Screener on left, Live Stock Detail on right!
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        RsScreenerScreen(
                                            stocks = screenerStocks,
                                            activePreset = activePreset,
                                            activeSort = activeSort,
                                            selectedSector = selectedSector,
                                            onPresetSelect = { viewModel.setScreenerPreset(it) },
                                            onSortSelect = { viewModel.setScreenerSort(it) },
                                            onSectorSelect = { viewModel.setScreenerSector(it) },
                                            onStockClick = { stock -> viewModel.openStockDetail(stock) }
                                        )
                                    }

                                    val previewStock = selectedStock ?: screenerStocks.firstOrNull()
                                    if (previewStock != null) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                                        ) {
                                            StockDetailScreen(
                                                stock = previewStock,
                                                savedThesis = activeStockThesis,
                                                onSaveThesis = { thesis, catalysts, target, risks ->
                                                    viewModel.saveThesis(
                                                        symbol = previewStock.symbol,
                                                        companyName = previewStock.name,
                                                        thesisText = thesis,
                                                        catalysts = catalysts,
                                                        targetValuation = target,
                                                        riskFactors = risks
                                                    )
                                                },
                                                onBack = { viewModel.closeStockDetail() }
                                            )
                                        }
                                    }
                                }
                            }

                            TerminalTab.EARNINGS -> {
                                EarningsRadarScreen(
                                    upcomingEarnings = upcomingEarnings,
                                    results = earningsResults,
                                    activeFilter = earningsFilter,
                                    onFilterSelect = { viewModel.setEarningsFilter(it) },
                                    onStockClick = { symbol -> viewModel.openStockBySymbol(symbol) }
                                )
                            }

                            TerminalTab.SAVED_NOTES -> {
                                SavedThesesScreen(
                                    theses = savedTheses,
                                    onSelectStock = { symbol -> viewModel.openStockBySymbol(symbol) },
                                    onDeleteThesis = { symbol -> viewModel.deleteThesis(symbol) }
                                )
                            }

                            TerminalTab.WEB_TERMINAL -> {
                                WebTerminalScreen()
                            }
                        }
                    }
                }
            }
        } else {
            // MOBILE COMPACT LAYOUT (Bottom Navigation Bar)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = TerminalBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    if (selectedStock == null) {
                        TerminalHeader(
                            nifty50Level = pulseData.nifty50Level,
                            nifty50Change = pulseData.nifty50Change,
                            nifty50ChangePercent = pulseData.nifty50ChangePercent,
                            nifty500Level = pulseData.nifty500Level,
                            nifty500Change = pulseData.nifty500Change,
                            nifty500ChangePercent = pulseData.nifty500ChangePercent,
                            isDarkTheme = isDarkTheme,
                            isLiveStreaming = isLiveStreaming,
                            streamTick = streamTick,
                            onToggleStream = { viewModel.toggleStreaming() },
                            onToggleTheme = { viewModel.toggleTheme() },
                            onSearchClick = { viewModel.setSearchOpen(true) },
                            onRefreshClick = { viewModel.refreshAll() }
                        )
                    }
                },
                bottomBar = {
                    if (selectedStock == null) {
                        TerminalBottomNavBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (selectedStock != null) {
                        StockDetailScreen(
                            stock = selectedStock!!,
                            savedThesis = activeStockThesis,
                            onSaveThesis = { thesis, catalysts, target, risks ->
                                viewModel.saveThesis(
                                    symbol = selectedStock!!.symbol,
                                    companyName = selectedStock!!.name,
                                    thesisText = thesis,
                                    catalysts = catalysts,
                                    targetValuation = target,
                                    riskFactors = risks
                                )
                            },
                            onBack = { viewModel.closeStockDetail() }
                        )
                    } else {
                        when (currentTab) {
                            TerminalTab.PULSE -> {
                                MarketPulseScreen(
                                    pulseData = pulseData,
                                    onStockClick = { symbol -> viewModel.openStockBySymbol(symbol) }
                                )
                            }

                            TerminalTab.INDICES -> {
                                IndexMatrixScreen(
                                    indices = indices,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategoryFilter(it) }
                                )
                            }

                            TerminalTab.SCREENER -> {
                                RsScreenerScreen(
                                    stocks = screenerStocks,
                                    activePreset = activePreset,
                                    activeSort = activeSort,
                                    selectedSector = selectedSector,
                                    onPresetSelect = { viewModel.setScreenerPreset(it) },
                                    onSortSelect = { viewModel.setScreenerSort(it) },
                                    onSectorSelect = { viewModel.setScreenerSector(it) },
                                    onStockClick = { stock -> viewModel.openStockDetail(stock) }
                                )
                            }

                            TerminalTab.EARNINGS -> {
                                EarningsRadarScreen(
                                    upcomingEarnings = upcomingEarnings,
                                    results = earningsResults,
                                    activeFilter = earningsFilter,
                                    onFilterSelect = { viewModel.setEarningsFilter(it) },
                                    onStockClick = { symbol -> viewModel.openStockBySymbol(symbol) }
                                )
                            }

                            TerminalTab.SAVED_NOTES -> {
                                SavedThesesScreen(
                                    theses = savedTheses,
                                    onSelectStock = { symbol -> viewModel.openStockBySymbol(symbol) },
                                    onDeleteThesis = { symbol -> viewModel.deleteThesis(symbol) }
                                )
                            }

                            TerminalTab.WEB_TERMINAL -> {
                                WebTerminalScreen()
                            }
                        }
                    }
                }
            }
        }

        // Global Search Dialog (Overlay)
        if (isSearchOpen) {
            GlobalSearchDialog(
                query = searchQuery,
                results = searchResults,
                allStocks = allStocks,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onStockSelect = { stock -> viewModel.openStockDetail(stock) },
                onDismiss = { viewModel.setSearchOpen(false) }
            )
        }
    }
}

@Composable
fun TerminalNavigationRail(
    currentTab: TerminalTab,
    isDarkTheme: Boolean,
    onTabSelected: (TerminalTab) -> Unit,
    onToggleTheme: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .width(80.dp)
            .border(1.dp, TerminalBorder),
        containerColor = TerminalSurface,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BullishGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "MarketEdge",
                        tint = TerminalSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "EDGE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            }
        }
    ) {
        val items = listOf(
            Triple(TerminalTab.PULSE, Icons.Default.TrendingUp, "Pulse"),
            Triple(TerminalTab.INDICES, Icons.Default.GridView, "Indices"),
            Triple(TerminalTab.SCREENER, Icons.Default.QueryStats, "Screener"),
            Triple(TerminalTab.EARNINGS, Icons.Default.Assessment, "Earnings"),
            Triple(TerminalTab.SAVED_NOTES, Icons.Default.Bookmarks, "Theses"),
            Triple(TerminalTab.WEB_TERMINAL, Icons.Default.Language, "Web")
        )

        Spacer(modifier = Modifier.height(16.dp))

        items.forEach { (tab, icon, label) ->
            val isSelected = currentTab == tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CyanRS else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyanRS else TextSecondary
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = CyanRSDark,
                    selectedIconColor = CyanRS,
                    unselectedIconColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_rail_${tab.name}")
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Theme Toggle in Navigation Rail
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .testTag("rail_theme_toggle_button")
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle theme",
                tint = AmberGold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TerminalBottomNavBar(
    currentTab: TerminalTab,
    onTabSelected: (TerminalTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder),
        containerColor = TerminalSurface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(TerminalTab.PULSE, Icons.Default.TrendingUp, "Pulse"),
            Triple(TerminalTab.INDICES, Icons.Default.GridView, "Indices"),
            Triple(TerminalTab.SCREENER, Icons.Default.QueryStats, "Screener"),
            Triple(TerminalTab.EARNINGS, Icons.Default.Assessment, "Earnings"),
            Triple(TerminalTab.SAVED_NOTES, Icons.Default.Bookmarks, "Theses"),
            Triple(TerminalTab.WEB_TERMINAL, Icons.Default.Language, "Web")
        )

        items.forEach { (tab, icon, label) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CyanRS else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyanRS else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyanRSDark,
                    selectedIconColor = CyanRS,
                    unselectedIconColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name}")
            )
        }
    }
}
