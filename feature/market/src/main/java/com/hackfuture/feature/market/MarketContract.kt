package com.hackfuture.feature.market

import com.hackfuture.core.model.CandleData
import com.hackfuture.core.model.CandleInterval
import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.OrderBookLevel

data class MarketState(
    val isLoading: Boolean = false,
    val marketDataList: List<MarketData> = emptyList(),
    val selectedSymbol: String = "BTCUSDT",
    val selectedInterval: CandleInterval = CandleInterval.H1,
    val klineData: List<CandleData> = emptyList(),
    val depthBids: List<OrderBookLevel> = emptyList(),
    val depthAsks: List<OrderBookLevel> = emptyList(),
    val recentTrades: List<MarketTrade> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val isKlineLoading: Boolean = false,
) {
    val filteredData: List<MarketData>
        get() = if (searchQuery.isBlank()) marketDataList
        else marketDataList.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
        }
    val maxBidVolume: Double get() = depthBids.maxOfOrNull { it.quantity } ?: 1.0
    val maxAskVolume: Double get() = depthAsks.maxOfOrNull { it.quantity } ?: 1.0
    val intervalLabels: List<Pair<CandleInterval, String>> get() = listOf(
        CandleInterval.M1 to "1分",
        CandleInterval.M5 to "5分",
        CandleInterval.M15 to "15分",
        CandleInterval.M30 to "30分",
        CandleInterval.H1 to "1小时",
        CandleInterval.H4 to "4小时",
        CandleInterval.D1 to "日线",
        CandleInterval.W1 to "周线",
    )
}

sealed class MarketIntent {
    data object LoadMarketData : MarketIntent()
    data class SelectSymbol(val symbol: String) : MarketIntent()
    data class SelectInterval(val interval: CandleInterval) : MarketIntent()
    data class LoadKline(val symbol: String, val interval: CandleInterval) : MarketIntent()
    data class Search(val query: String) : MarketIntent()
    data object Refresh : MarketIntent()
}

sealed class MarketEffect {
    data class ShowError(val message: String) : MarketEffect()
    data class NavigateToTrading(val symbol: String) : MarketEffect()
}

data class MarketTrade(
    val price: Double,
    val quantity: Double,
    val isBuyerMaker: Boolean,
    val timestamp: Long,
)

object MarketSymbols {
    val DEFAULT = listOf(
        "BTCUSDT" to "BTC/USDT",
        "ETHUSDT" to "ETH/USDT",
        "BNBUSDT" to "BNB/USDT",
        "SOLUSDT" to "SOL/USDT",
        "XRPUSDT" to "XRP/USDT",
        "DOGEUSDT" to "DOGE/USDT",
    )
}
