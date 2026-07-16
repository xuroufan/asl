package com.hackfuture.feature.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.OrderSide
import com.hackfuture.core.model.OrderType
import com.hackfuture.core.model.PlaceOrderRequest
import com.hackfuture.core.network.ApiService
import com.hackfuture.core.network.websocket.WebSocketEvent
import com.hackfuture.core.network.websocket.WebSocketManager
import kotlinx.serialization.json.Json
import com.hackfuture.core.model.Order

import com.hackfuture.core.model.ApiResult
import com.hackfuture.core.model.parseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

private const val DEBOUNCE_MS = 300L
private const val SUBMIT_COOLDOWN_MS = 2000L

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val apiService: ApiService,
    private val webSocketManager: WebSocketManager,
    private val json: Json,
) : ViewModel() {

    private val _state = MutableStateFlow(TradingState())
    val state: StateFlow<TradingState> = _state.asStateFlow()
    private val _effect = Channel<TradingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

   private var lastSubmitTime = 0L
   private var debounceJob: Job? = null
    private var subscribedSymbol: String? = null

   init {
        loadMarketData()
        observeWebSocket()
    }

    fun onIntent(intent: TradingIntent) {
        when (intent) {
            is TradingIntent.SetSymbol -> setSymbol(intent.symbol)
            is TradingIntent.SetSide -> _state.update { it.copy(orderSide = intent.side) }
            is TradingIntent.SetOrderType -> _state.update { it.copy(orderType = intent.type) }
            is TradingIntent.SetPrice -> _state.update { it.copy(price = intent.price) }
            is TradingIntent.SetQuantity -> _state.update { it.copy(quantity = intent.quantity) }
            is TradingIntent.PreviewOrder -> showConfirmDialog()
            is TradingIntent.ConfirmOrder -> placeOrder()
            is TradingIntent.DismissConfirm -> _state.update { it.copy(showConfirmDialog = false, confirmOrder = null) }
            is TradingIntent.PlaceOrder -> { /* direct submit bypass, not used */ }
            is TradingIntent.LoadMarketData -> loadMarketData()
            is TradingIntent.ClearMessage -> _state.update { it.copy(successMessage = null, error = null) }
        }
    }

   private fun setSymbol(symbol: String) {
        val oldSymbol = _state.value.symbol
        if (oldSymbol != symbol) {
            webSocketManager.unsubscribe("${oldSymbol.lowercase()}@ticker")
        }
        _state.update { it.copy(symbol = symbol) }
        webSocketManager.subscribe("${symbol.lowercase()}@ticker")
        subscribedSymbol = symbol
        loadMarketData()
    }

    private fun loadMarketData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val tickerResult = apiService.getTicker(_state.value.symbol)
                val data = tickerResult.parseData<MarketData>(json)
                if (data != null) {
                    _state.update { it.copy(isLoading = false, marketData = data, currentPrice = data.price) }
                } else {
                    _state.update { it.copy(isLoading = false, error = tickerResult.msg.ifEmpty { "获取行情失败" }) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Load market failed")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ==================== WebSocket 实时价格 ====================

    private fun observeWebSocket() {
        viewModelScope.launch {
            val symbol = _state.value.symbol
            subscribedSymbol = symbol
            webSocketManager.subscribe("${symbol.lowercase()}@ticker")
            webSocketManager.events.collect { event ->
                if (event.type == WebSocketEvent.EventType.MESSAGE) {
                    event.data?.let { updatePriceFromWs(it) }
                }
            }
        }
    }

    private fun updatePriceFromWs(message: String) {
        try {
            val json = JSONObject(message).optJSONObject("data") ?: return
            val price = json.optDouble("price", 0.0)
            if (price > 0) {
                _state.update { it.copy(currentPrice = price) }
            }
        } catch (_: Exception) {}
    }

    // ==================== 二次确认弹窗 ====================

    private fun showConfirmDialog() {
        val s = _state.value
        val price = if (s.orderType == OrderType.MARKET) s.currentPrice
            else s.price.toDoubleOrNull() ?: return
        val qty = s.quantity.toDoubleOrNull() ?: return
        if (price <= 0 || qty <= 0) {
            _state.update { it.copy(error = "请输入有效的价格和数量") }
            return
        }
        _state.update {
            it.copy(
                showConfirmDialog = true,
                confirmOrder = PendingOrder(
                    symbol = s.symbol,
                    side = s.orderSide,
                    type = s.orderType,
                    price = price,
                    quantity = qty,
                    total = price * qty,
                ),
            )
        }
    }

    // ==================== 防抖下单 ====================

    private fun placeOrder() {
        val now = System.currentTimeMillis()
        if (now - lastSubmitTime < SUBMIT_COOLDOWN_MS) {
            Timber.w("Submit too fast, throttled")
            _effect.trySend(TradingEffect.ShowError("操作过于频繁，请稍后再试"))
            return
        }
        lastSubmitTime = now

        val s = _state.value
        val price = if (s.orderType == OrderType.MARKET) s.currentPrice
            else s.price.toDoubleOrNull() ?: return
        val qty = s.quantity.toDoubleOrNull() ?: return

        _state.update { it.copy(isSubmitting = true, showConfirmDialog = false, error = null) }

        viewModelScope.launch {
            try {
                val orderResult = apiService.placeOrder(
                    PlaceOrderRequest(
                        symbol = s.symbol,
                        type = s.orderType,
                        side = s.orderSide,
                        price = if (s.orderType != OrderType.MARKET) price else null,
                        quantity = qty,
                    ),
                )
                val order = orderResult.parseData<Order>(json)
                if (order != null) {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            price = "",
                            quantity = "",
                            confirmOrder = null,
                            successMessage = "订单已提交: ${order.id}",
                        )
                    }
                    _effect.send(TradingEffect.OrderPlaced(order))
                } else {
                    throw Exception(orderResult.msg.ifEmpty { "下单失败" })
                }
            } catch (e: Exception) {
                Timber.e(e, "Order failed")
                _state.update { it.copy(isSubmitting = false, error = e.message) }
                _effect.send(TradingEffect.ShowError(e.message ?: "下单失败"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscribedSymbol?.let { webSocketManager.unsubscribe("${it.lowercase()}@ticker") }
    }
}
