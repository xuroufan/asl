package com.hackfuture.feature.position

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackfuture.core.model.AccountBalance
import com.hackfuture.core.model.ClosePositionRequest
import com.hackfuture.core.model.Position
import com.hackfuture.core.network.ApiService
import com.hackfuture.core.network.websocket.WebSocketEvent
import com.hackfuture.core.network.websocket.WebSocketManager
import kotlinx.serialization.json.Json
import com.hackfuture.core.model.ApiResult
import com.hackfuture.core.model.parseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PositionViewModel @Inject constructor(
    private val apiService: ApiService,
    private val webSocketManager: WebSocketManager,
    private val json: Json,
) : ViewModel() {

    private val _state = MutableStateFlow(PositionState())
    val state: StateFlow<PositionState> = _state.asStateFlow()
    private val _effect = Channel<PositionEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val livePrices = mutableMapOf<String, Double>()

    init {
        loadPositions()
        observeWebSocket()
    }

    fun onIntent(intent: PositionIntent) {
        when (intent) {
            is PositionIntent.LoadPositions -> loadPositions()
            is PositionIntent.SelectPosition -> _state.update { it.copy(selectedPosition = it.positions.find { p -> p.id == intent.positionId }) }
            is PositionIntent.ClosePosition -> closePosition(intent.positionId)
            is PositionIntent.Refresh -> loadPositions()
            is PositionIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadPositions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val posResult = apiService.getPositions()
                val balResult = apiService.getBalances()
                val positions = posResult.parseData<List<Position>>(json) ?: emptyList()
                val balances = balResult.parseData<List<AccountBalance>>(json) ?: emptyList()
                val updated = applyLivePrices(positions)
                val cap = buildSummary(updated, balances)
                _state.update {
                    it.copy(isLoading = false, positions = updated, balances = balances, summary = cap)
                }
                subscribePriceChannels(updated)
            } catch (e: Exception) {
                Timber.e(e, "Load positions failed")
                _state.update { it.copy(isLoading = false, error = e.message) }
                _effect.send(PositionEffect.ShowError(e.message ?: "加载失败"))
            }
        }
    }

    private fun closePosition(positionId: String) {
        val pos = _state.value.positions.find { it.id == positionId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(isClosingPosition = true) }
            try {
                val closeResult = apiService.closePosition(ClosePositionRequest(positionId, pos.symbol))
                if (closeResult.code == 200) {
                    _state.update { it.copy(isClosingPosition = false, selectedPosition = null) }
                    _effect.send(PositionEffect.PositionClosed(positionId))
                } else {
                    throw Exception(closeResult.msg.ifEmpty { "平仓失败" })
                }
                loadPositions()
            } catch (e: Exception) {
                Timber.e(e, "Close position failed")
                _state.update { it.copy(isClosingPosition = false, error = e.message) }
                _effect.send(PositionEffect.ShowError(e.message ?: "平仓失败"))
            }
        }
    }

    // ==================== WebSocket 实时价格 ====================

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                if (event.type == WebSocketEvent.EventType.MESSAGE) {
                    event.data?.let { onWsMessage(it) }
                }
            }
        }
    }

    private fun subscribePriceChannels(positions: List<Position>) {
        positions.forEach { pos ->
            webSocketManager.subscribe("${pos.symbol.lowercase()}@ticker")
        }
    }

    private fun onWsMessage(message: String) {
        try {
            val json = JSONObject(message)
            val channel = json.optString("channel", "")
            val data = json.optJSONObject("data") ?: return
            if (channel.endsWith("@ticker")) {
                val symbol = data.optString("symbol", "")
                val price = data.optDouble("price", 0.0)
                if (symbol.isNotEmpty() && price > 0) {
                    livePrices[symbol] = price
                    recalculatePnl()
                }
            }
        } catch (_: Exception) {}
    }

    private fun recalculatePnl() {
        val updated = _state.value.positions.map { pos ->
            val livePrice = livePrices[pos.symbol]
            if (livePrice != null) {
                val pnl = (livePrice - pos.entryPrice) * pos.quantity * (if (pos.side == com.hackfuture.core.model.PositionSide.LONG) 1 else -1)
                val pnlPercent = if (pos.entryPrice > 0) (pnl / (pos.entryPrice * pos.quantity)) * 100 else 0.0
                pos.copy(currentPrice = livePrice, unrealizedPnl = pnl, unrealizedPnlPercent = pnlPercent)
            } else pos
        }
        val cap = buildSummary(updated)
        _state.update { it.copy(positions = updated, summary = cap) }
    }

    private fun applyLivePrices(positions: List<Position>): List<Position> {
        return positions.map { pos ->
            val live = livePrices[pos.symbol]
            if (live != null) {
                val pnl = (live - pos.entryPrice) * pos.quantity * (if (pos.side == com.hackfuture.core.model.PositionSide.LONG) 1 else -1)
                val pp = if (pos.entryPrice > 0) (pnl / (pos.entryPrice * pos.quantity)) * 100 else 0.0
                pos.copy(currentPrice = live, unrealizedPnl = pnl, unrealizedPnlPercent = pp)
            } else pos
        }
    }

    private fun buildSummary(positions: List<Position>, balances: List<AccountBalance> = _state.value.balances): CapitalSummary {
        val margin = positions.sumOf { it.marginUsed }
        val pnl = positions.sumOf { it.unrealizedPnl }
        val avail = balances.firstOrNull { it.asset == "USDT" }?.available ?: 0.0
        val equity = avail + margin + pnl
        return CapitalSummary(
            totalEquity = equity,
            available = avail,
            marginUsed = margin,
            unrealizedPnl = pnl,
            riskRatio = if (margin > 0) (equity / margin) * 100 else 100.0,
            positionCount = positions.size,
        )
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.positions.forEach { pos ->
            webSocketManager.unsubscribe("${pos.symbol.lowercase()}@ticker")
        }
    }
}
