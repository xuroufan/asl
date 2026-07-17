package com.hackfuture.feature.position

import com.hackfuture.core.model.AccountBalance
import com.hackfuture.core.model.Position
import com.hackfuture.core.model.PositionSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** PositionState 单元测试 — computed properties */
class PositionStateTest {

    private val longPos = Position(id="1", symbol="BTC", side=PositionSide.LONG,
        quantity=2.0, entryPrice=50000.0, currentPrice=55000.0, unrealizedPnl=10000.0,
        unrealizedPnlPercent=10.0, realizedPnl=500.0, marginUsed=20000.0, leverage=5,
        liquidationPrice=40000.0, openedAt=100L, updatedAt=200L)
    private val shortPos = Position(id="2", symbol="ETH", side=PositionSide.SHORT,
        quantity=10.0, entryPrice=3000.0, currentPrice=2800.0, unrealizedPnl=2000.0,
        unrealizedPnlPercent=6.67, realizedPnl=0.0, marginUsed=6000.0, leverage=5,
        liquidationPrice=3600.0, openedAt=150L, updatedAt=250L)

    @Test
    fun `totalPositions counts all positions`() {
        val state = PositionState(positions = listOf(longPos, shortPos))
        assertEquals(2, state.totalPositions)
    }

    @Test
    fun `totalUnrealizedPnl sums all positions`() {
        val state = PositionState(positions = listOf(longPos, shortPos))
        assertEquals(12000.0, state.totalUnrealizedPnl, 0.0001)
    }

    @Test
    fun `totalMarketValue sums market values`() {
        val state = PositionState(positions = listOf(longPos, shortPos))
        // 2 * 55000 + 10 * 2800 = 110000 + 28000 = 138000
        assertEquals(138000.0, state.totalMarketValue, 0.0001)
    }

    @Test
    fun `totalMarginUsed sums margins`() {
        val state = PositionState(positions = listOf(longPos, shortPos))
        assertEquals(26000.0, state.totalMarginUsed, 0.0001)
    }

    @Test
    fun `availableBalance returns USDT balance`() {
        val balances = listOf(
            AccountBalance(asset="USDT", free=5000.0, locked=1000.0, total=6000.0),
            AccountBalance(asset="BTC", free=0.5, locked=0.0, total=0.5),
        )
        val state = PositionState(positions = listOf(longPos), balances = balances)
        assertEquals(5000.0, state.availableBalance, 0.0001)
    }

    @Test
    fun `availableBalance defaults to zero when no USDT`() {
        val balances = listOf(AccountBalance(asset="BTC", free=1.0, locked=0.0, total=1.0))
        val state = PositionState(positions = listOf(longPos), balances = balances)
        assertEquals(0.0, state.availableBalance, 0.0001)
    }

    @Test
    fun `totalEquity includes balance margin and pnl`() {
        val balances = listOf(AccountBalance(asset="USDT", free=5000.0, locked=1000.0, total=6000.0))
        val state = PositionState(positions = listOf(longPos), balances = balances)
        // 5000 + 20000 + 10000 = 35000
        assertEquals(35000.0, state.totalEquity, 0.0001)
    }

    @Test
    fun `riskRatio calculates correctly`() {
        val state = PositionState(positions = listOf(longPos))
        val balances = listOf(AccountBalance(asset="USDT", free=10000.0, locked=0.0, total=10000.0))
        val s = state.copy(balances = balances)
        // (10000 + 20000 + 10000) / 20000 * 100 = 200
        assertEquals(200.0, s.riskRatio, 0.0001)
    }

    @Test
    fun `riskRatio defaults to 100 when no margin`() {
        val state = PositionState()
        assertEquals(100.0, state.riskRatio, 0.0001)
    }

    @Test
    fun `empty state has zero values`() {
        val state = PositionState()
        assertEquals(0, state.totalPositions)
        assertEquals(0.0, state.totalUnrealizedPnl, 0.0001)
        assertEquals(0.0, state.totalMarketValue, 0.0001)
    }

    @Test
    fun `selectedPosition can be set`() {
        val state = PositionState(positions = listOf(longPos, shortPos))
        assertNull(state.selectedPosition)
        val withSelected = state.copy(selectedPosition = longPos)
        assertEquals("1", withSelected.selectedPosition?.id)
    }
}
