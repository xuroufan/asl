package com.hackfuture.feature.market

import com.hackfuture.core.model.PriceDirection
import com.hackfuture.core.model.Quote
import com.hackfuture.core.model.QuoteCategory
import com.hackfuture.core.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MarketOverviewState 单元测试 — 筛选、排序、统计 */
class MarketOverviewStateTest {

    private val q1 = Quote(symbol="HK00700", name="腾讯", lastPrice=400.0, change=10.0,
        changePercent=2.5, open=390.0, high=405.0, low=389.0, volume=1_000_000L,
        category=QuoteCategory.STOCK)
    private val q2 = Quote(symbol="BTCUSDT", name="BTC", lastPrice=60000.0, change=-500.0,
        changePercent=-0.83, open=60500.0, high=61000.0, low=59000.0, volume=5000L,
        category=QuoteCategory.COMMODITY)
    private val q3 = Quote(symbol="HK00005", name="汇丰", lastPrice=75.0, change=0.0,
        changePercent=0.0, open=75.0, high=76.0, low=74.5, volume=2_000_000L,
        category=QuoteCategory.STOCK)

    @Test
    fun `displayQuotes shows all when no filter`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3))
        assertEquals(3, state.displayQuotes.size)
    }

    @Test
    fun `displayQuotes filters by category`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            selectedCategory = QuoteCategory.STOCK)
        assertEquals(2, state.displayQuotes.size)
        assertTrue(state.displayQuotes.all { it.category == QuoteCategory.STOCK })
    }

    @Test
    fun `displayQuotes filters by search query matching symbol`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            searchQuery = "BTC")
        assertEquals(1, state.displayQuotes.size)
        assertEquals("BTCUSDT", state.displayQuotes.first().symbol)
    }

    @Test
    fun `displayQuotes filters by search query matching name`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            searchQuery = "腾讯")
        assertEquals(1, state.displayQuotes.size)
        assertEquals("HK00700", state.displayQuotes.first().symbol)
    }

    @Test
    fun `displayQuotes search is case-insensitive`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            searchQuery = "btc")
        assertEquals(1, state.displayQuotes.size)
    }

    @Test
    fun `displayQuotes sorts by volume descending by default`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            sortMode = SortMode.VOLUME, sortDescending = true)
        assertEquals("HK00005", state.displayQuotes.first().symbol)
    }

    @Test
    fun `displayQuotes sorts by symbol ascending`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            sortMode = SortMode.SYMBOL, sortDescending = false)
        assertEquals("BTCUSDT", state.displayQuotes.first().symbol)
    }

    @Test
    fun `displayQuotes sorts by change percentage descending`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            sortMode = SortMode.CHANGE, sortDescending = true)
        assertEquals("HK00700", state.displayQuotes.first().symbol)
    }

    @Test
    fun `displayQuotes sorts by price ascending`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3),
            sortMode = SortMode.PRICE, sortDescending = false)
        assertEquals("HK00005", state.displayQuotes.first().symbol)
    }

    @Test
    fun `upCount downCount flatCount`() {
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3))
        assertEquals(1, state.upCount)
        assertEquals(1, state.downCount)
        assertEquals(1, state.flatCount)
    }

    @Test
    fun `empty quotes returns empty display`() {
        val state = MarketOverviewState()
        assertTrue(state.displayQuotes.isEmpty())
        assertEquals(0, state.upCount)
    }

    @Test
    fun `combined category and search filter`() {
        val q4 = Quote(symbol="BTCETH", name="ETH/BTC", lastPrice=0.05, change=0.001,
            changePercent=2.0, open=0.049, high=0.051, low=0.048, volume=100_000L,
            category=QuoteCategory.STOCK)
        val state = MarketOverviewState(quotes = listOf(q1, q2, q3, q4),
            selectedCategory = QuoteCategory.STOCK, searchQuery = "BTC")
        assertEquals(1, state.displayQuotes.size)
    }
}
