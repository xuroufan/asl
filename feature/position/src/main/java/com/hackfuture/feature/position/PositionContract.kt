package com.hackfuture.feature.position

import com.hackfuture.core.model.Position
import com.hackfuture.core.model.AccountBalance

data class PositionState(
    val isLoading: Boolean = false,
    val positions: List<Position> = emptyList(),
    val selectedPosition: Position? = null,
    val isClosingPosition: Boolean = false,
    val balances: List<AccountBalance> = emptyList(),
    val summary: CapitalSummary? = null,
    val error: String? = null,
) {
    val totalPositions: Int get() = positions.size
    val totalUnrealizedPnl: Double get() = positions.sumOf { it.unrealizedPnl }
    val totalMarketValue: Double get() = positions.sumOf { it.marketValue }
    val totalMarginUsed: Double get() = positions.sumOf { it.marginUsed }
    val availableBalance: Double
        get() = balances.firstOrNull { it.asset == "USDT" }?.available ?: 0.0
    val totalEquity: Double
        get() = availableBalance + totalMarginUsed + totalUnrealizedPnl
    val riskRatio: Double
        get() = if (totalMarginUsed > 0) (totalEquity / totalMarginUsed) * 100 else 100.0
}

data class CapitalSummary(
    val totalEquity: Double,
    val available: Double,
    val marginUsed: Double,
    val unrealizedPnl: Double,
    val riskRatio: Double,
    val positionCount: Int,
)

sealed class PositionIntent {
    data object LoadPositions : PositionIntent()
    data class SelectPosition(val positionId: String) : PositionIntent()
    data class ClosePosition(val positionId: String) : PositionIntent()
    data object Refresh : PositionIntent()
    data object ClearError : PositionIntent()
}

sealed class PositionEffect {
    data class ShowError(val message: String) : PositionEffect()
    data class PositionClosed(val positionId: String) : PositionEffect()
    data class NavigateToTrading(val symbol: String) : PositionEffect()
}
