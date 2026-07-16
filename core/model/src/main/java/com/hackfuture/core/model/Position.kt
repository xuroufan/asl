package com.hackfuture.core.model

import kotlinx.serialization.Serializable

/**
 * 持仓信息
 */
@Serializable
data class Position(
    val id: String,
    val symbol: String,
    val side: PositionSide,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPercent: Double,
    val realizedPnl: Double,
    val marginUsed: Double,
    val leverage: Int = 1,
    val liquidationPrice: Double? = null,
    val openedAt: Long,
    val updatedAt: Long,
) {
    val marketValue: Double
        get() = quantity * currentPrice

    val pnlDirection: PriceDirection
        get() = when {
            unrealizedPnl > 0 -> PriceDirection.UP
            unrealizedPnl < 0 -> PriceDirection.DOWN
            else -> PriceDirection.FLAT
        }
}

@Serializable
enum class PositionSide {
    LONG,
    SHORT,
}

/**
 * 仓位汇总
 */
@Serializable
data class PositionSummary(
    val totalPositions: Int,
    val totalUnrealizedPnl: Double,
    val totalRealizedPnl: Double,
    val totalMarginUsed: Double,
    val totalMarketValue: Double,
    val positions: List<Position>,
)

/**
 * 仓位关闭请求
 */
@Serializable
data class ClosePositionRequest(
    val positionId: String,
    val symbol: String,
    val quantity: Double? = null,
)
