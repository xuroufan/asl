package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 实时行情数据
 */
@Serializable
data class MarketData(
    val symbol: String,
    val name: String,
    val exchange: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val close: Double,
    val volume: Long,
    val timestamp: Long,
) {
    val priceDirection: PriceDirection
        get() = when {
            change > 0 -> PriceDirection.UP
            change < 0 -> PriceDirection.DOWN
            else -> PriceDirection.FLAT
        }
}

@Serializable
enum class PriceDirection {
    UP,
    DOWN,
    FLAT,
}

/**
 * K线数据
 */
@Serializable
data class CandleData(
    val symbol: String,
    val interval: CandleInterval,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val timestamp: Long,
)

@Serializable
enum class CandleInterval {
    M1,
    M5,
    M15,
    M30,
    H1,
    H4,
    D1,
    W1,
}

/**
 * 行情订阅请求
 */
@Serializable
data class MarketSubscription(
    val symbols: List<String>,
    val channels: List<MarketChannel>,
)

@Serializable
enum class MarketChannel {
    TICKER,
    ORDER_BOOK,
    TRADE,
    CANDLE,
}

/**
 * 深度行情
 */
@Serializable
data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val timestamp: Long,
)

@Serializable
data class OrderBookLevel(
    val price: Double,
    val quantity: Double,
    val orderCount: Int = 0,
)

/**
 * 市场统计
 */
@Serializable
data class MarketStats(
    val symbol: String,
    val marketCap: Double? = null,
    val volume24h: Double,
    val high24h: Double,
    val low24h: Double,
    val changePercent24h: Double,
)
