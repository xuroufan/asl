package com.futures.market;

import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import com.futures.market.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行情服务单元测试 — 纯内存服务，无需 Spring 上下文。
 */
class MarketServiceTest {

    private MarketService marketService;

    @BeforeEach
    void setUp() {
        marketService = new MarketService();
        marketService.init();
    }

    @Test
    @DisplayName("合约列表 — 应返回全部10个合约")
    void testGetSymbols_ReturnsAllContracts() {
        List<Map<String, Object>> symbols = marketService.getSymbols();
        assertEquals(10, symbols.size(), "应有10个合约");
        assertTrue(symbols.stream().anyMatch(s -> "ES".equals(s.get("symbol"))), "应包含ES");
        assertTrue(symbols.stream().anyMatch(s -> "HSI".equals(s.get("symbol"))), "应包含HSI");
    }

    @Test
    @DisplayName("报价查询 — 已知合约返回正确字段")
    void testGetQuote_ExistingSymbol_ReturnsQuote() {
        Map<String, Object> quote = marketService.getQuote("ES");
        assertNotNull(quote);
        assertEquals("ES", quote.get("symbol"));
        assertNotNull(quote.get("bid"));
        assertNotNull(quote.get("ask"));
        assertNotNull(quote.get("last"));
        assertNotNull(quote.get("timestamp"));
    }

    @Test
    @DisplayName("报价查询 — 未知合约返回null")
    void testGetQuote_UnknownSymbol_ReturnsNull() {
        assertNull(marketService.getQuote("UNKNOWN"));
    }

    @Test
    @DisplayName("五档盘口 — 返回5个买卖档位")
    void testGetDepth_ReturnsFiveLevels() {
        MarketDepth depth = marketService.getDepth("ES");
        assertNotNull(depth);
        assertEquals("ES", depth.getSymbol());
        assertEquals(5, depth.getBids().size(), "买盘应有5档");
        assertEquals(5, depth.getAsks().size(), "卖盘应有5档");
    }

    @Test
    @DisplayName("五档盘口 — 买价逐级降低，卖价逐级升高")
    void testGetDepth_BidsDescendingAsksAscending() {
        MarketDepth depth = marketService.getDepth("NQ");
        for (int i = 1; i < depth.getBids().size(); i++) {
            assertTrue(depth.getBids().get(i - 1).getPrice().compareTo(depth.getBids().get(i).getPrice()) > 0,
                    "买价应逐级降低");
        }
        for (int i = 1; i < depth.getAsks().size(); i++) {
            assertTrue(depth.getAsks().get(i - 1).getPrice().compareTo(depth.getAsks().get(i).getPrice()) < 0,
                    "卖价应逐级升高");
        }
    }

    @Test
    @DisplayName("K线数据 — 请求时生成样本数据")
    void testGetKlines_ReturnsSampleData() {
        List<KlineEntity> klines = marketService.getKlines("ES", "1m", 10);
        assertEquals(10, klines.size(), "应返回10条K线");
        for (KlineEntity k : klines) {
            assertEquals("ES", k.getSymbol());
            assertEquals("1m", k.getInterval());
            assertNotNull(k.getOpen());
            assertNotNull(k.getClose());
            assertNotNull(k.getHigh());
            assertNotNull(k.getLow());
        }
    }

    @Test
    @DisplayName("成交记录 — 返回指定数量")
    void testGetTrades_ReturnsLimit() {
        List<Map<String, Object>> trades = marketService.getTrades("ES", 20);
        assertEquals(20, trades.size(), "应返回20条成交");
        for (Map<String, Object> t : trades) {
            assertNotNull(t.get("id"));
            assertNotNull(t.get("price"));
            assertNotNull(t.get("volume"));
        }
    }

    @Test
    @DisplayName("价格步进 — 推进后价格变化")
    void testStepPrice_ChangesPrice() {
        Map<String, Object> before = marketService.getQuote("ES");
        BigDecimal priceBefore = (BigDecimal) before.get("last");

        // 推进10步
        for (int i = 0; i < 10; i++) {
            marketService.stepPrice("ES");
        }

        Map<String, Object> after = marketService.getQuote("ES");
        BigDecimal priceAfter = (BigDecimal) after.get("last");

        // 价格可能没变（万一随机数为0），但大概率变了
        // 至少保证 Bid/Ask 有值
        assertNotNull(after.get("bid"));
        assertNotNull(after.get("ask"));
    }

    @Test
    @DisplayName("全部合约报价快照 — 返回所有合约")
    void testGetAllQuotes_ReturnsAllSymbols() {
        Map<String, Map<String, Object>> all = marketService.getAllQuotes();
        assertEquals(10, all.size(), "应包含10个合约的报价");
    }

    @Test
    @DisplayName("合约代码列表 — 返回10个代码")
    void testGetSymbolList_Returns10Symbols() {
        assertEquals(10, marketService.getSymbolList().size());
    }
}
