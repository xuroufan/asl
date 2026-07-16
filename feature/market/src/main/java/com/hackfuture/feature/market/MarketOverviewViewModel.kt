package com.hackfuture.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.Quote
import com.hackfuture.core.model.QuoteCategory
import com.hackfuture.core.model.SortMode
import com.hackfuture.core.network.ApiService
import com.hackfuture.core.network.websocket.WebSocketEvent
import com.hackfuture.core.network.websocket.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.hackfuture.core.network.alltick.AllTickApiService
import com.hackfuture.core.network.alltick.toWatchlistQuote
import com.hackfuture.core.network.alltick.toAllTickSymbol
import timber.log.Timber
import javax.inject.Inject

/**
 * 行情看板 ViewModel — 管理合约列表的加载、实时更新、搜索/筛选/排序。
 */
@HiltViewModel
class MarketOverviewViewModel @Inject constructor(
    private val apiService: ApiService,
    private val allTickApiService: AllTickApiService,
    private val webSocketManager: WebSocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MarketOverviewState())
    val state: StateFlow<MarketOverviewState> = _state.asStateFlow()

    private val _effect = Channel<MarketOverviewEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** WebSocket 已订阅的 channels，用于断线重连后恢复 */
    private val subscribedChannels = mutableSetOf<String>()

    init {
        loadData()
        observeWebSocket()
    }

    fun onIntent(intent: MarketOverviewIntent) {
        when (intent) {
            is MarketOverviewIntent.LoadData -> loadData()
            is MarketOverviewIntent.Refresh -> loadData()
            is MarketOverviewIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is MarketOverviewIntent.SelectCategory -> {
                _state.update { it.copy(selectedCategory = intent.category) }
            }
            is MarketOverviewIntent.SelectSort -> {
                val current = _state.value.sortMode
                _state.update {
                    it.copy(
                        sortMode = intent.mode,
                        sortDescending = if (current == intent.mode) !it.sortDescending else true,
                    )
                }
            }
            is MarketOverviewIntent.ToggleSortDirection -> {
                _state.update { it.copy(sortDescending = !it.sortDescending) }
            }
            is MarketOverviewIntent.SelectSymbol -> {
                viewModelScope.launch {
                    _effect.send(MarketOverviewEffect.NavigateToDetail(intent.symbol))
                }
            }
        }
    }

    // ==================== 初始加载 ====================

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Try AllTick real-time for all symbols
                val allTickResp = allTickApiService.getRealTime()
                val data = allTickResp.data
                if (allTickResp.ret == 0 && data.isNotEmpty()) {
                    val quotes = allTickResp.data.map { it.toWatchlistQuote() }
                    _state.update {
                        it.copy(isLoading = false, quotes = quotes, lastUpdated = System.currentTimeMillis())
                    }
                    subscribeSymbols(quotes)
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Timber.e(e, "加载行情列表失败")
                _state.update { it.copy(isLoading = false, error = e.message) }
                _effect.send(MarketOverviewEffect.ShowError(e.message ?: "加载行情失败"))
            }
        }
    }

    /** 订阅所有合约的实时 ticker */
    private fun subscribeSymbols(quotes: List<Quote>) {
        quotes.forEach { q ->
            val channel = "${q.symbol.lowercase()}@ticker"
            if (subscribedChannels.add(channel)) {
                webSocketManager.subscribe(channel)
            }
        }
    }

    // ==================== WebSocket 实时更新 ====================

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event.type) {
                    WebSocketEvent.EventType.CONNECTED -> {
                        _state.update { it.copy(isConnected = true) }
                        // 断线重连后恢复订阅
                        subscribedChannels.forEach { webSocketManager.subscribe(it) }
                    }
                    WebSocketEvent.EventType.MESSAGE -> {
                        event.data?.let { handleWsMessage(it) }
                    }
                    WebSocketEvent.EventType.DISCONNECTED -> {
                        _state.update { it.copy(isConnected = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    /** 解析 WebSocket 消息并更新对应合约的报价 */
    private fun handleWsMessage(message: String) {
        try {
            val json = JSONObject(message)
            val channel = json.optString("channel", "")
            if (!channel.endsWith("@ticker")) return
            val data = json.optJSONObject("data") ?: return
            val symbol = data.optString("symbol", "") ?: return
            if (symbol.isEmpty()) return

            val price = data.optDouble("price", 0.0)
            val change = data.optDouble("change", 0.0)
            val changePercent = data.optDouble("changePercent", 0.0)
            val high = data.optDouble("high", 0.0)
            val low = data.optDouble("low", 0.0)
            val volume = data.optLong("volume", 0L)
            val open = data.optDouble("open", 0.0)

            _state.update { s ->
                val updatedQuotes = s.quotes.map { q ->
                    if (q.symbol == symbol) {
                        q.copy(
                            lastPrice = if (price > 0) price else q.lastPrice,
                            change = change,
                            changePercent = changePercent,
                            high = high.coerceAtLeast(q.high),
                            low = if (low > 0) low.coerceAtMost(if (q.low > 0) q.low else low) else q.low,
                            open = if (open > 0) open else q.open,
                            volume = if (volume > 0) volume else q.volume,
                        )
                    } else q
                }
                s.copy(quotes = updatedQuotes, lastUpdated = System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Timber.v("WS解析跳过: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscribedChannels.forEach { webSocketManager.unsubscribe(it) }
        subscribedChannels.clear()
    }
}

// ==================== 数据映射 ====================

/**
 * 将 [MarketData]（API 返回）映射为 UI 层更简洁的 [Quote] 模型。
 */
private fun MarketData.toQuote(): Quote {
    val category = when {
        symbol.startsWith("HK") && symbol.length == 7 && symbol.drop(2).all { it.isDigit() } ->
            QuoteCategory.STOCK
        symbol in listOf("AU", "AG", "CU", "SC") -> QuoteCategory.COMMODITY
        else -> QuoteCategory.STOCK
    }
    return Quote(
        symbol = symbol,
        name = name.ifEmpty { symbol },
        exchange = exchange,
        category = category,
        lastPrice = price,
        change = change,
        changePercent = changePercent,
        open = open,
        high = high,
        low = low,
        volume = volume,
    )
}
