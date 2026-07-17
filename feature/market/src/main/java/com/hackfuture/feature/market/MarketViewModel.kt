package com.hackfuture.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackfuture.core.model.CandleInterval
import com.hackfuture.core.model.OrderBookLevel
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
import org.json.JSONArray
import org.json.JSONObject
import com.hackfuture.core.network.alltick.AllTickApiService
import com.hackfuture.core.network.alltick.toAllTickSymbol
import com.hackfuture.core.network.alltick.toAllTickType
import com.hackfuture.core.network.alltick.toMarketData
import com.hackfuture.core.network.alltick.toCandleData
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val apiService: ApiService,
    private val allTickApiService: AllTickApiService,
    private val webSocketManager: WebSocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MarketState())
    val state: StateFlow<MarketState> = _state.asStateFlow()

    private val _effect = Channel<MarketEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadMarketData()
        observeWebSocket()
    }

    fun onIntent(intent: MarketIntent) {
        when (intent) {
            is MarketIntent.LoadMarketData -> loadMarketData()
            is MarketIntent.SelectSymbol -> selectSymbol(intent.symbol)
            is MarketIntent.SelectInterval -> selectInterval(intent.interval)
            is MarketIntent.LoadKline -> loadKline(intent.symbol, intent.interval)
            is MarketIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is MarketIntent.Refresh -> loadMarketData()
        }
    }

    // ==================== 行情数据 ====================

    private fun loadMarketData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Try backend API
                val result = apiService.getAllQuotes()
                if (result.code == 200) {
                    val json = com.hackfuture.core.network.di.NetworkModule.provideJson()
                    val data = result.parseDataList<com.hackfuture.core.model.MarketData>(json)
                    if (data != null) {
                        _state.update { it.copy(isLoading = false, marketDataList = data) }
                        return@launch
                    }
                }
                // 2. Fallback: AllTick real-time API
                val symbol = _state.value.selectedSymbol
                val allTickResp = allTickApiService.getRealTime(symbol = symbol.toAllTickSymbol())
                if (allTickResp.ret == 0 && allTickResp.data.isNotEmpty()) {
                    val md = allTickResp.data.first().toMarketData()
                    _state.update { it.copy(isLoading = false, marketDataList = listOf(md)) }
                }
                webSocketManager.subscribe("${symbol.lowercase()}@ticker")
                webSocketManager.subscribe("${symbol.lowercase()}@depth20")
                webSocketManager.subscribe("${symbol.lowercase()}@trade")
                loadKline(symbol, _state.value.selectedInterval)
            } catch (e: Exception) {
                Timber.e(e, "Load market failed")
                _state.update { it.copy(isLoading = false, error = e.message) }
                _effect.send(MarketEffect.ShowError(e.message ?: "加载失败"))
            }
        }
    }

    private fun selectSymbol(symbol: String) {
        val old = _state.value.selectedSymbol
        webSocketManager.unsubscribe("${old.lowercase()}@ticker")
        webSocketManager.unsubscribe("${old.lowercase()}@depth20")
        webSocketManager.unsubscribe("${old.lowercase()}@trade")
        _state.update { it.copy(selectedSymbol = symbol) }
        webSocketManager.subscribe("${symbol.lowercase()}@ticker")
        webSocketManager.subscribe("${symbol.lowercase()}@depth20")
        webSocketManager.subscribe("${symbol.lowercase()}@trade")
        loadKline(symbol, _state.value.selectedInterval)
    }

    private fun selectInterval(interval: CandleInterval) {
        _state.update { it.copy(selectedInterval = interval) }
        loadKline(_state.value.selectedSymbol, interval)
    }

    private fun loadKline(symbol: String, interval: CandleInterval) {
        viewModelScope.launch {
            _state.update { it.copy(isKlineLoading = true) }
            try {
                val atSymbol = symbol.toAllTickSymbol()
                val atType = interval.toAllTickType()
                val response = allTickApiService.getKline(symbol = atSymbol, type = atType, count = 200)
                if (response.ret == 0 && response.data.isNotEmpty()) {
                    val candles = response.data.map { it.toCandleData(symbol) }
                    _state.update { it.copy(klineData = preprocessKline(candles), isKlineLoading = false) }
                } else {
                    // Fallback: try daily kline
                    val dailyResp = allTickApiService.getKline(symbol = atSymbol, type = "D", count = 100)
                val data = dailyResp.data
                if (dailyResp.ret == 0 && data.isNotEmpty()) {
                        val candles = dailyResp.data.map { it.toCandleData(symbol) }
                        _state.update { it.copy(klineData = preprocessKline(candles), isKlineLoading = false) }
                    } else {
                        Timber.w("AllTick returned no kline data for $symbol")
                        _state.update { it.copy(isKlineLoading = false) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Load kline failed for $symbol")
                _state.update { it.copy(isKlineLoading = false) }
            }
        }
    }

    // ==================== K线数据预处理 ====================

    private fun preprocessKline(data: List<com.hackfuture.core.model.CandleData>): List<com.hackfuture.core.model.CandleData> {
        return data.sortedBy { it.timestamp }
    }

    // ==================== WebSocket 数据流 ====================

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event.type) {
                    WebSocketEvent.EventType.MESSAGE -> {
                        event.data?.let { handleWsMessage(it) }
                    }
                    WebSocketEvent.EventType.CONNECTED -> {
                        val s = _state.value.selectedSymbol
                        webSocketManager.subscribe("${s.lowercase()}@ticker")
                        webSocketManager.subscribe("${s.lowercase()}@depth20")
                        webSocketManager.subscribe("${s.lowercase()}@trade")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleWsMessage(message: String) {
        try {
            val json = JSONObject(message)
            val channel = json.optString("channel", "")
            val data = json.optJSONObject("data") ?: return
            when {
                channel.endsWith("@ticker") -> handleTickerUpdate(data)
                channel.endsWith("@depth20") -> handleDepthUpdate(data)
                channel.endsWith("@trade") -> handleTradeUpdate(data)
            }
        } catch (e: Exception) {
            Timber.w("WS parse error: ${e.message}")
        }
    }

    private fun handleTickerUpdate(data: JSONObject) {
        val symbol = data.optString("symbol", _state.value.selectedSymbol)
        val price = data.optDouble("price", 0.0)
        val change = data.optDouble("change", 0.0)
        val changePercent = data.optDouble("changePercent", 0.0)
        _state.update { s ->
            val updatedList = s.marketDataList.map { md ->
                if (md.symbol == symbol) md.copy(
                    price = price, change = change, changePercent = changePercent,
                ) else md
            }
            s.copy(marketDataList = updatedList)
        }
    }

    private fun handleDepthUpdate(data: JSONObject) {
        val bids = parseOrderBookLevels(data.optJSONArray("bids"))
        val asks = parseOrderBookLevels(data.optJSONArray("asks"))
        _state.update { it.copy(depthBids = bids.take(5), depthAsks = asks.take(5)) }
    }

    private fun handleTradeUpdate(data: JSONObject) {
        val trade = MarketTrade(
            price = data.optDouble("price", 0.0),
            quantity = data.optDouble("quantity", 0.0),
            isBuyerMaker = data.optBoolean("isBuyerMaker", true),
            timestamp = data.optLong("timestamp", System.currentTimeMillis()),
        )
        _state.update { it.copy(recentTrades = (listOf(trade) + it.recentTrades).take(50)) }
    }

    private fun parseOrderBookLevels(arr: JSONArray?): List<OrderBookLevel> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONArray(i)
            OrderBookLevel(
                price = item.optDouble(0, 0.0),
                quantity = item.optDouble(1, 0.0),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        val s = _state.value.selectedSymbol
        webSocketManager.unsubscribe("${s.lowercase()}@ticker")
        webSocketManager.unsubscribe("${s.lowercase()}@depth20")
        webSocketManager.unsubscribe("${s.lowercase()}@trade")
    }
}
