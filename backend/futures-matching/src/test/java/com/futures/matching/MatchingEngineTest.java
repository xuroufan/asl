package com.futures.matching;

import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.engine.MatchingEngine;
import com.futures.matching.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 撮合引擎核心逻辑单元测试。
 */
class MatchingEngineTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine("ES");
    }

    /** 挂入对手盘（使用 engine 正常下单，价格错开避免成交） */
    private void addOpponent(Order.Direction dir, long price, int volume) {
        // 处理对手单（不成交的对手盘会挂在订单簿中）
        // 注意：这里会对引擎状态产生 side effect
        Order order = Order.builder()
                .orderId("OPP-" + System.nanoTime())
                .userId("2")
                .symbol("ES")
                .direction(dir)
                .type(Order.OrderType.LIMIT)
                .price(price)
                .volume(volume)
                .filledVolume(0)
                .timestamp(System.nanoTime())
                .build();
        engine.processOrder(order);
    }

    // ==================== 限价单测试 ====================

    @Test
    void testLimitBuy_WithOpponentSell_ShouldMatch() {
        addOpponent(Order.Direction.SELL, 4500, 2);

        Order buyOrder = Order.builder()
                .orderId("B1").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.LIMIT)
                .price(4500).volume(1).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(buyOrder);

        assertTrue(result.isHasTrades());
        assertEquals("FILLED", result.getFinalStatus());
        assertEquals(1, result.getTotalFilledVolume());
    }

    @Test
    void testLimitBuy_NoOpponent_ShouldHang() {
        Order buyOrder = Order.builder()
                .orderId("B2").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.LIMIT)
                .price(4490).volume(1).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(buyOrder);

        assertFalse(result.isHasTrades());
        assertEquals("PENDING", result.getFinalStatus());
        assertEquals(1, engine.getOrderBook().getTotalOrderCount()); // 挂单成功
    }

    @Test
    void testLimitSell_PartialFill() {
        addOpponent(Order.Direction.BUY, 4500, 3); // 买方总量3手

        Order sellOrder = Order.builder()
                .orderId("S1").userId("1").symbol("ES")
                .direction(Order.Direction.SELL).type(Order.OrderType.LIMIT)
                .price(4500).volume(5).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(sellOrder);

        assertTrue(result.isHasTrades());
        assertEquals(3, result.getTotalFilledVolume());
        assertEquals(2, result.getRemainingVolume());
        assertEquals("PARTIAL", result.getFinalStatus());
    }

    // ==================== 市价单测试 ====================

    @Test
    void testMarketBuy_ShouldMatchAtBestAsk() {
        addOpponent(Order.Direction.SELL, 4510, 2);
        addOpponent(Order.Direction.SELL, 4520, 3);

        Order buyOrder = Order.builder()
                .orderId("MB1").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.MARKET)
                .price(0).volume(2).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(buyOrder);

        assertTrue(result.isHasTrades());
        assertEquals(2, result.getTotalFilledVolume());
        assertEquals("FILLED", result.getFinalStatus());
    }

    @Test
    void testMarketBuy_NoLiquidity_ShouldCancel() {
        Order buyOrder = Order.builder()
                .orderId("MB2").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.MARKET)
                .price(0).volume(2).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(buyOrder);

        assertFalse(result.isHasTrades());
        assertEquals("PENDING", result.getFinalStatus()); // 市价单未成交，状态保持PENDING
    }

    // ==================== FOK 测试 ====================

    @Test
    void testFOK_WhenVolumeMatchable_ShouldFill() {
        addOpponent(Order.Direction.SELL, 4500, 5);

        Order fokOrder = Order.builder()
                .orderId("FOK1").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.FOK)
                .price(4500).volume(5).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(fokOrder);

        assertTrue(result.isHasTrades());
        assertEquals("FILLED", result.getFinalStatus());
        assertEquals(5, result.getTotalFilledVolume());
    }

    @Test
    void testFOK_WhenVolumeUnmatchable_ShouldKill() {
        addOpponent(Order.Direction.SELL, 4500, 3);

        Order fokOrder = Order.builder()
                .orderId("FOK2").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.FOK)
                .price(4500).volume(5).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(fokOrder);

        assertFalse(result.isHasTrades());
    }

    // ==================== IOC 测试 ====================

    @Test
    void testIOC_ShouldFillPartialThenCancel() {
        addOpponent(Order.Direction.SELL, 4500, 2);

        Order iocOrder = Order.builder()
                .orderId("IOC1").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.IOC)
                .price(4500).volume(5).filledVolume(0).timestamp(System.nanoTime())
                .build();

        MatchedOrder result = engine.processOrder(iocOrder);

        assertTrue(result.isHasTrades());
        assertEquals(2, result.getTotalFilledVolume());
    }

    // ==================== 撤单测试 ====================

    @Test
    void testCancelPendingOrder_ShouldRemoveFromBook() {
        Order order = Order.builder()
                .orderId("CNCL1").userId("1").symbol("ES")
                .direction(Order.Direction.BUY).type(Order.OrderType.LIMIT)
                .price(4490).volume(2).filledVolume(0).timestamp(System.nanoTime())
                .build();

        // 挂单（不成交）
        engine.processOrder(order);

        assertEquals(1, engine.getOrderBook().getTotalOrderCount());

        boolean cancelled = engine.cancelOrder("CNCL1");
        assertTrue(cancelled);
        assertEquals(0, engine.getOrderBook().getTotalOrderCount());
    }
}
