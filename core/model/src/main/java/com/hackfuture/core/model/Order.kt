package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 交易订单
 */
@Serializable
data class Order(
    val id: String,
    val symbol: String,
    val type: OrderType,
    val side: OrderSide,
    val status: OrderStatus,
    val price: Double? = null,
    val stopPrice: Double? = null,
    val quantity: Double,
    val filledQuantity: Double = 0.0,
    val averageFilledPrice: Double? = null,
    val totalAmount: Double,
    val fee: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val remainingQuantity: Double
        get() = quantity - filledQuantity

    val isFullyFilled: Boolean
        get() = status == OrderStatus.FILLED

    val isActive: Boolean
        get() = status == OrderStatus.PENDING || status == OrderStatus.PARTIALLY_FILLED
}

@Serializable
enum class OrderType {
    MARKET,
    LIMIT,
    STOP,
    STOP_LIMIT,
}

@Serializable
enum class OrderSide {
    BUY,
    SELL,
}

@Serializable
enum class OrderStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED,
}

/**
 * 下单请求
 */
@Serializable
data class PlaceOrderRequest(
    val symbol: String,
    val type: OrderType,
    val side: OrderSide,
    val price: Double? = null,
    val stopPrice: Double? = null,
    val quantity: Double,
)

/**
 * 撤单请求
 */
@Serializable
data class CancelOrderRequest(
    val orderId: String,
    val symbol: String,
)

@Serializable
data class OrderHistoryItem(
    val orderId: String,
    val symbol: String,
    val side: OrderSide,
    val price: Double,
    val quantity: Double,
    val totalAmount: Double,
    val fee: Double,
    val executedAt: Long,
)
