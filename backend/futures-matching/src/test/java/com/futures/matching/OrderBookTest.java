package com.futures.matching;

import com.futures.matching.engine.OrderBook;
import com.futures.matching.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单簿单元测试（基于 engine.OrderBook）。
 */
class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    private Order makeOrder(String id, Order.Direction dir, Order.OrderType type,
                             long price, int volume) {
        return Order.builder()
                .orderId(id).userId("1").symbol("ES")
                .direction(dir).type(type)
                .price(price).volume(volume)
                .filledVolume(0).timestamp(System.nanoTime())
                .build();
    }

    @Test
    void testAddLimitBuyOrder_ShouldAppearInBids() {
        Order order = makeOrder("B1", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 2);
        orderBook.add(order, true);

        assertTrue(orderBook.getBidLevelCount() > 0);
        assertEquals(1, orderBook.getTotalOrderCount());
        assertEquals(4500L, orderBook.getBestBidPrice().longValue());
    }

    @Test
    void testAddLimitSellOrder_ShouldAppearInAsks() {
        Order order = makeOrder("S1", Order.Direction.SELL, Order.OrderType.LIMIT, 4510, 3);
        orderBook.add(order, false);

        assertTrue(orderBook.getAskLevelCount() > 0);
        assertEquals(1, orderBook.getTotalOrderCount());
        assertEquals(4510L, orderBook.getBestAskPrice().longValue());
    }

    @Test
    void testCancelOrder_ShouldRemoveFromOrderBook() {
        Order order = makeOrder("B1", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 2);
        orderBook.add(order, true);

        assertTrue(orderBook.cancelOrder("B1"));
        assertEquals(0, orderBook.getTotalOrderCount());
    }

    @Test
    void testCancelNonExistentOrder_ShouldReturnFalse() {
        assertFalse(orderBook.cancelOrder("NONEXISTENT"));
    }

    @Test
    void testMultiplePriceLevels_BidsDescending() {
        orderBook.add(makeOrder("B1", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 1), true);
        orderBook.add(makeOrder("B2", Order.Direction.BUY, Order.OrderType.LIMIT, 4510, 1), true);
        orderBook.add(makeOrder("B3", Order.Direction.BUY, Order.OrderType.LIMIT, 4490, 1), true);

        // 买盘：价格从高到低：4510, 4500, 4490
        assertEquals(3, orderBook.getBidLevelCount());
        assertEquals(4510L, orderBook.getBestBidPrice().longValue()); // 最高价优先
    }

    @Test
    void testSamePriceOrders_FIFO() {
        Order buy1 = makeOrder("B1", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 1);
        Order buy2 = makeOrder("B2", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 1);

        orderBook.add(buy1, true);
        orderBook.add(buy2, true);

        Deque<Order> level = orderBook.getBidsAtPrice(4500);
        assertNotNull(level);
        assertEquals(2, level.size());
        assertEquals("B1", level.peekFirst().getOrderId()); // FIFO: 先入先出
    }

    @Test
    void testBidAskIndependence() {
        Order buy = makeOrder("B1", Order.Direction.BUY, Order.OrderType.LIMIT, 4500, 2);
        Order sell = makeOrder("S1", Order.Direction.SELL, Order.OrderType.LIMIT, 4510, 3);

        orderBook.add(buy, true);
        orderBook.add(sell, false);

        assertEquals(1, orderBook.getBidLevelCount());
        assertEquals(1, orderBook.getAskLevelCount());
        assertEquals(2, orderBook.getTotalOrderCount());
        assertEquals(2, orderBook.getBidLevelCount() + orderBook.getAskLevelCount());
    }

    @Test
    void testEmptyOrderBook() {
        assertTrue(orderBook.getBidLevelCount() == 0 && orderBook.getAskLevelCount() == 0);
        assertNull(orderBook.getBestBidPrice());
        assertNull(orderBook.getBestAskPrice());
        assertEquals(0, orderBook.getTotalOrderCount());
    }
}
