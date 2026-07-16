package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 成交记录
 */
@Serializable
data class Trade(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val price: Double,
    val quantity: Double,
    val totalAmount: Double,
    val fee: Double,
    val feeCurrency: String = "USDT",
    val orderId: String,
    val executedAt: Long,
)

/**
 * 账户资产
 */
@Serializable
data class AccountBalance(
    val asset: String,
    val free: Double,
    val locked: Double,
    val total: Double,
) {
    val available: Double
        get() = free
}

/**
 * 资金流水
 */
@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val asset: String,
    val amount: Double,
    val fee: Double = 0.0,
    val status: TransactionStatus,
    val description: String? = null,
    val createdAt: Long,
)

@Serializable
enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRADE_FEE,
    TRANSFER_IN,
    TRANSFER_OUT,
}

@Serializable
enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

/**
 * 分页请求
 */
@Serializable
data class PageRequest(
    val page: Int = 1,
    val size: Int = 20,
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.DESC,
)

@Serializable
enum class SortOrder {
    ASC,
    DESC,
}

/**
 * 分页响应
 */
@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val size: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean
        get() = page < totalPages

    val isEmpty: Boolean
        get() = items.isEmpty()

    companion object {
        fun <T> empty(): PageResponse<T> = PageResponse(
            items = emptyList(),
            total = 0,
            page = 1,
            size = 20,
            totalPages = 0,
        )
    }
}

/**
 * WebSocket 消息封装
 */
@Serializable
data class WsMessage<T>(
    val channel: String,
    val data: T,
    val timestamp: Long,
)
