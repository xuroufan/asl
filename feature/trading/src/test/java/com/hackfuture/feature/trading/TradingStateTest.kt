package com.hackfuture.feature.trading

import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.OrderSide
import com.hackfuture.core.model.OrderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TradingState 单元测试 */
class TradingStateTest {

    @Test
    fun `default state values`() {
        val state = TradingState()
        assertEquals("BTCUSDT", state.symbol)
        assertEquals(OrderSide.BUY, state.orderSide)
        assertEquals(OrderType.LIMIT, state.orderType)
        assertFalse(state.isLoading)
        assertFalse(state.isSubmitting)
        assertFalse(state.showConfirmDialog)
        assertEquals("", state.price)
        assertEquals("", state.quantity)
    }

    @Test
    fun `canSubmit false when submitting`() {
        val state = TradingState(isSubmitting = true, price = "100", quantity = "1")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit false when quantity is blank`() {
        val state = TradingState(quantity = "")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit false when quantity is zero`() {
        val state = TradingState(quantity = "0")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit false when quantity is invalid`() {
        val state = TradingState(quantity = "abc")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit true for market order with valid quantity`() {
        val state = TradingState(orderType = OrderType.MARKET, quantity = "1.5", currentPrice = 100.0)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `canSubmit false for limit order with blank price`() {
        val state = TradingState(orderType = OrderType.LIMIT, quantity = "1", price = "")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit false for limit order with zero price`() {
        val state = TradingState(orderType = OrderType.LIMIT, quantity = "1", price = "0")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit true for limit order with valid price and quantity`() {
        val state = TradingState(orderType = OrderType.LIMIT, quantity = "2", price = "50000")
        assertTrue(state.canSubmit)
    }

    @Test
    fun `canSubmit false for stop order with blank price`() {
        val state = TradingState(orderType = OrderType.STOP, quantity = "1", price = "")
        assertFalse(state.canSubmit)
    }

    @Test
    fun `marketData can be set`() {
        val md = MarketData(symbol="BTC", name="", exchange="", price=100.0,
            change=0.0, changePercent=0.0, high=110.0, low=90.0, open=100.0,
            close=100.0, volume=1000L, timestamp=0L)
        val state = TradingState(marketData = md)
        assertEquals(100.0, state.marketData!!.price, 0.0001)
        assertNull(state.error)
    }

    @Test
    fun `canSubmit reflects isSubmitting and price quantity`() {
        assertFalse(TradingState(price = "100", quantity = "1", isSubmitting = true).canSubmit)
        assertTrue(TradingState(price = "100", quantity = "1", isSubmitting = false).canSubmit)
        assertFalse(TradingState(price = "100", quantity = "0", isSubmitting = false).canSubmit)
    }
}
