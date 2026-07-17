package com.hackfuture.core.network.alltick

import com.hackfuture.core.model.CandleInterval
import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.PriceDirection
import com.hackfuture.core.model.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AllTick 工具类单元测试 — 验证 symbol 转换和模型映射 */
class AllTickUtilTest {

    // ==================== Symbol 转换 ====================

    @Test
    fun `toAllTickSymbol converts HK stock code`() {
        assertEquals("HK.00700", "HK00700".toAllTickSymbol())
        assertEquals("HK.00005", "HK00005".toAllTickSymbol())
    }

    @Test
    fun `toAllTickSymbol handles short symbols`() {
        assertEquals("AB", "AB".toAllTickSymbol())
        // HK-style prefix split for non-HK symbols: BTCUSDT → BT.CUSDT
        assertEquals("BT.CUSDT", "BTCUSDT".toAllTickSymbol())
    }

    @Test
    fun `fromAllTickSymbol removes dot`() {
        assertEquals("HK00700", "HK.00700".fromAllTickSymbol())
        assertEquals("BTCUSDT", "BTCUSDT".fromAllTickSymbol())
    }

    @Test
    fun `toAllTickSymbol and fromAllTickSymbol are invertible`() {
        val symbols = listOf("HK00700", "HK00005", "BTCUSDT", "ETHUSDT")
        for (s in symbols) {
            assertEquals(s, s.toAllTickSymbol().fromAllTickSymbol())
        }
    }

    // ==================== CandleInterval 映射 ====================

    @Test
    fun `toAllTickType maps all intervals`() {
        assertEquals("1", CandleInterval.M1.toAllTickType())
        assertEquals("5", CandleInterval.M5.toAllTickType())
        assertEquals("15", CandleInterval.M15.toAllTickType())
        assertEquals("30", CandleInterval.M30.toAllTickType())
        assertEquals("60", CandleInterval.H1.toAllTickType())
        assertEquals("240", CandleInterval.H4.toAllTickType())
        assertEquals("D", CandleInterval.D1.toAllTickType())
        assertEquals("W", CandleInterval.W1.toAllTickType())
    }

    // ==================== toMarketData ====================

    @Test
    fun `toMarketData maps all fields correctly`() {
        val quote = AllTickQuote(
            symbol = "HK.00700",
            timestamp = 1786946096000L,
            open = 100.0,
            high = 110.0,
            low = 95.0,
            close = 105.0,
            volume = 1000000L,
            turnover = 100000000.0,
            change = 5.0,
            changePercent = 5.0,
        )
        val md = quote.toMarketData()

        assertEquals("HK00700", md.symbol)
        assertEquals("腾讯控股", md.name)
        assertEquals("HKEX", md.exchange)
        assertEquals(105.0, md.price, 0.0001)
        assertEquals(5.0, md.change, 0.0001)
        assertEquals(5.0, md.changePercent, 0.0001)
        assertEquals(100.0, md.open, 0.0001)
        assertEquals(110.0, md.high, 0.0001)
        assertEquals(95.0, md.low, 0.0001)
        assertEquals(105.0, md.close, 0.0001)
        assertEquals(1000000L, md.volume)
        assertEquals(1786946096000L, md.timestamp)
    }

    @Test
    fun `toMarketData uses symbol as name for unknown stock`() {
        val quote = AllTickQuote(
            symbol = "BTC.USDT", timestamp = 1L, open = 0.0, high = 0.0, low = 0.0,
            close = 100.0, volume = 0L, turnover = 0.0, change = 0.0, changePercent = 0.0,
        )
        assertEquals("BTCUSDT", quote.toMarketData().name)
    }

    @Test
    fun `toMarketData sets price direction from change`() {
        val q = AllTickQuote(symbol="HK.00700", timestamp=1L, open=0.0, high=0.0, low=0.0,
            close=100.0, volume=0L, turnover=0.0, change=5.0, changePercent=5.0)
        assertEquals(PriceDirection.UP, q.toMarketData().priceDirection)
    }

    // ==================== toWatchlistQuote ====================

    @Test
    fun `toWatchlistQuote maps correctly`() {
        val atq = AllTickQuote(
            symbol = "HK.00700",
            timestamp = 1L,
            open = 100.0,
            high = 110.0,
            low = 95.0,
            close = 105.0,
            volume = 1000L,
            turnover = 100000.0,
            change = 5.0,
            changePercent = 5.0,
        )
        val q = atq.toWatchlistQuote()
        assertEquals("HK00700", q.symbol)
        assertEquals(105.0, q.lastPrice, 0.0001)
        assertEquals(5.0, q.change, 0.0001)
        assertEquals(100.0, q.open, 0.0001)
    }

    // ==================== toCandleData ====================

    @Test
    fun `toCandleData maps correctly`() {
        val ak = AllTickKline(
            symbol = "BTC.USDT", open = 100.0, high = 110.0, low = 90.0, close = 105.0,
            volume = 1000L, turnover = 100000.0, timestamp = 100L,
        )
        val cd = ak.toCandleData("BTCUSDT")
        assertEquals("BTCUSDT", cd.symbol)
        assertEquals(100.0, cd.open, 0.0001)
        assertEquals(110.0, cd.high, 0.0001)
        assertEquals(90.0, cd.low, 0.0001)
        assertEquals(105.0, cd.close, 0.0001)
        assertEquals(1000L, cd.volume)
        assertEquals(CandleInterval.D1, cd.interval)
    }
}
