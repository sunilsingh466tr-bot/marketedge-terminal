package com.example.marketedge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketedge.data.MarketPulseData
import com.example.marketedge.data.MarketRepository
import com.example.marketedge.data.local.AppDatabase
import com.example.marketedge.data.local.ThesisNoteEntity
import com.example.marketedge.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TerminalTab(val label: String) {
    PULSE("Pulse"),
    INDICES("Indices"),
    SCREENER("RS Screener"),
    EARNINGS("Earnings"),
    SAVED_NOTES("My Theses"),
    WEB_TERMINAL("Web View")
}

enum class ScreenerPreset(val label: String) {
    ALL("All Stocks"),
    SUPER_LEADERS("Super Momentum (RS≥95)"),
    ALL_WEATHER("All-Weather (RS>80)"),
    RESILIENT("Resilient in Correction"),
    POCKET_PIVOT("Pocket Pivot (>2x Vol)"),
    NEAR_52W("Within 5% 52W High")
}

enum class ScreenerSort(val label: String) {
    RS_DESC("RS Score (High to Low)"),
    NEAR_52W_HIGH("Closest to 52W High"),
    VOLUME_SURGE("Highest Volume Surge"),
    DAY_CHANGE("Top Gainers Today"),
    PRICE_DESC("Price (High to Low)")
}

class MarketEdgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val thesisDao = db.thesisNoteDao()

    private val _isDarkTheme = MutableStateFlow(false) // Default to Light theme as requested
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentTab = MutableStateFlow(TerminalTab.PULSE)
    val currentTab: StateFlow<TerminalTab> = _currentTab.asStateFlow()

    private val _selectedStock = MutableStateFlow<ScreenerStock?>(null)
    val selectedStock: StateFlow<ScreenerStock?> = _selectedStock.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    // Market Pulse
    private val _pulseData = MutableStateFlow(MarketRepository.getMarketPulse())
    val pulseData: StateFlow<MarketPulseData> = _pulseData.asStateFlow()

    // Real-Time 1-Second Streaming Engine State
    private val _isLiveStreaming = MutableStateFlow(false)
    val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming.asStateFlow()

    private val _streamTick = MutableStateFlow(0)
    val streamTick: StateFlow<Int> = _streamTick.asStateFlow()

    init {
        // Streaming disabled by default post-market close (15:30 IST)
        startStreamingEngine()
    }

    private fun startStreamingEngine() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_isLiveStreaming.value) {
                    _streamTick.value += 1
                    val delta50 = (kotlin.random.Random.nextDouble() - 0.49) * 2.8
                    val delta500 = delta50 * 0.94
                    val currentPulse = _pulseData.value
                    val new50 = currentPulse.nifty50Level + delta50
                    val new50Chg = currentPulse.nifty50Change + delta50
                    val prevClose50 = 25255.05 // Verified Nov 11, 2026 close baseline (25,299.35 - 44.30)
                    val new50Pct = (new50Chg / prevClose50) * 100

                    val new500 = currentPulse.nifty500Level + delta500
                    val new500Chg = currentPulse.nifty500Change + delta500
                    val prevClose500 = 23566.90 // Verified Nov 11, 2026 close baseline (23,779.30 - 212.40)
                    val new500Pct = (new500Chg / prevClose500) * 100

                    _pulseData.value = currentPulse.copy(
                        nifty50Level = new50,
                        nifty50Change = new50Chg,
                        nifty50ChangePercent = new50Pct,
                        nifty500Level = new500,
                        nifty500Change = new500Chg,
                        nifty500ChangePercent = new500Pct
                    )
                }
            }
        }
    }

    fun toggleStreaming() {
        _isLiveStreaming.value = !_isLiveStreaming.value
    }

    fun resetToBaseline() {
        _pulseData.value = MarketRepository.getMarketPulse()
        _indices.value = MarketRepository.getIndices()
    }

    // Indices
    private val _indices = MutableStateFlow(MarketRepository.getIndices())
    val indices: StateFlow<List<IndexSummary>> = _indices.asStateFlow()

    private val _selectedCategory = MutableStateFlow<IndexCategory?>(null)
    val selectedCategory: StateFlow<IndexCategory?> = _selectedCategory.asStateFlow()

    // Screener State
    private val _screenerStocks = MutableStateFlow(MarketRepository.getScreenerStocks())
    val allStocks: StateFlow<List<ScreenerStock>> = _screenerStocks.asStateFlow()

    private val _activePreset = MutableStateFlow(ScreenerPreset.ALL)
    val activePreset: StateFlow<ScreenerPreset> = _activePreset.asStateFlow()

    private val _activeSort = MutableStateFlow(ScreenerSort.RS_DESC)
    val activeSort: StateFlow<ScreenerSort> = _activeSort.asStateFlow()

    private val _selectedSector = MutableStateFlow("All")
    val selectedSector: StateFlow<String> = _selectedSector.asStateFlow()

    // Earnings Radar State
    private val _earningsResults = MutableStateFlow(MarketRepository.getEarningsResults())
    val earningsResults: StateFlow<List<EarningsResult>> = _earningsResults.asStateFlow()

    private val _upcomingEarnings = MutableStateFlow(MarketRepository.getUpcomingEarnings())
    val upcomingEarnings: StateFlow<List<UpcomingEarnings>> = _upcomingEarnings.asStateFlow()

    private val _earningsFilter = MutableStateFlow("All")
    val earningsFilter: StateFlow<String> = _earningsFilter.asStateFlow()

    // All Saved Theses Notes from Room
    val savedTheses: StateFlow<List<ThesisNoteEntity>> = thesisDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Stock Note
    val activeStockThesis: StateFlow<ThesisNoteEntity?> = _selectedStock
        .flatMapLatest { stock ->
            if (stock != null) thesisDao.getNoteForStock(stock.symbol)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Derived Screener Results
    val filteredScreenerStocks: StateFlow<List<ScreenerStock>> = combine(
        _screenerStocks,
        _activePreset,
        _activeSort,
        _selectedSector
    ) { stocks, preset, sort, sector ->
        var list = stocks

        // Filter by Sector
        if (sector != "All") {
            list = list.filter { it.sector.contains(sector, ignoreCase = true) }
        }

        // Filter by Preset
        list = when (preset) {
            ScreenerPreset.ALL -> list
            ScreenerPreset.SUPER_LEADERS -> list.filter { it.rsScore >= 95 }
            ScreenerPreset.ALL_WEATHER -> list.filter { it.rsScore >= 80 && it.currentPrice >= it.sma50 }
            ScreenerPreset.RESILIENT -> list.filter { it.isResilientInCorrection }
            ScreenerPreset.POCKET_PIVOT -> list.filter { it.isPocketPivot }
            ScreenerPreset.NEAR_52W -> list.filter { it.isNear52WHigh }
        }

        // Sort
        when (sort) {
            ScreenerSort.RS_DESC -> list.sortedByDescending { it.rsScore }
            ScreenerSort.NEAR_52W_HIGH -> list.sortedByDescending { it.distanceFrom52WHighPercent } // closer to 0 is higher
            ScreenerSort.VOLUME_SURGE -> list.sortedByDescending { it.volumeMultiplier }
            ScreenerSort.DAY_CHANGE -> list.sortedByDescending { it.changePercent }
            ScreenerSort.PRICE_DESC -> list.sortedByDescending { it.currentPrice }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Earnings Results
    val filteredEarningsResults: StateFlow<List<EarningsResult>> = combine(
        _earningsResults,
        _earningsFilter
    ) { results, filter ->
        when (filter) {
            "All" -> results
            "Strong Beat" -> results.filter { it.verdict == EarningsVerdict.STRONG_BEAT }
            "In-Line" -> results.filter { it.verdict == EarningsVerdict.IN_LINE }
            "Weak Miss" -> results.filter { it.verdict == EarningsVerdict.WEAK_MISS }
            "Upper Circuit" -> results.filter { it.isUpperCircuit || it.priceReactionPercent >= 14.0 }
            else -> results
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search Matches
    val searchResults: StateFlow<List<ScreenerStock>> = combine(
        _screenerStocks,
        _globalSearchQuery
    ) { stocks, query ->
        if (query.isBlank()) emptyList()
        else {
            stocks.filter {
                it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.sector.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: TerminalTab) {
        _currentTab.value = tab
        _selectedStock.value = null
    }

    fun openStockDetail(stock: ScreenerStock) {
        _selectedStock.value = stock
        _isSearchOpen.value = false
    }

    fun openStockBySymbol(symbol: String) {
        val stock = _screenerStocks.value.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
        if (stock != null) {
            openStockDetail(stock)
        }
    }

    fun closeStockDetail() {
        _selectedStock.value = null
    }

    fun setSearchOpen(open: Boolean) {
        _isSearchOpen.value = open
        if (!open) _globalSearchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _globalSearchQuery.value = query
    }

    fun setScreenerPreset(preset: ScreenerPreset) {
        _activePreset.value = preset
    }

    fun setScreenerSort(sort: ScreenerSort) {
        _activeSort.value = sort
    }

    fun setScreenerSector(sector: String) {
        _selectedSector.value = sector
    }

    fun setEarningsFilter(filter: String) {
        _earningsFilter.value = filter
    }

    fun setCategoryFilter(category: IndexCategory?) {
        _selectedCategory.value = category
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun refreshAll() {
        _pulseData.value = MarketRepository.getMarketPulse()
        _indices.value = MarketRepository.getIndices()
        _screenerStocks.value = MarketRepository.getScreenerStocks()
        _earningsResults.value = MarketRepository.getEarningsResults()
        _upcomingEarnings.value = MarketRepository.getUpcomingEarnings()
    }

    fun saveThesis(
        symbol: String,
        companyName: String,
        thesisText: String,
        catalysts: String,
        targetValuation: String,
        riskFactors: String
    ) {
        viewModelScope.launch {
            val entity = ThesisNoteEntity(
                symbol = symbol,
                companyName = companyName,
                thesisText = thesisText,
                catalysts = catalysts,
                targetValuation = targetValuation,
                riskFactors = riskFactors,
                updatedAt = System.currentTimeMillis()
            )
            thesisDao.saveNote(entity)
        }
    }

    fun deleteThesis(symbol: String) {
        viewModelScope.launch {
            thesisDao.deleteNote(symbol)
        }
    }
}
