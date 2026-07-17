package com.futures.matching.engine;

import com.futures.matching.model.Order;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 订单簿 — 基于 ConcurrentSkipListMap 实现
 * <p>
 * 买盘（bids）：按价格从高到低排序（反向比较器）
 * 卖盘（asks）：按价格从低到高排序（自然顺序）
 * 同一价格级别使用 FIFO 队列（先到先成交）
 */
@Slf4j
public class OrderBook {

    /** 买盘：价格从高到低，value=该价位的订单队列（FIFO） */
    private final ConcurrentSkipListMap<Long, Deque<Order>> bids;

    /** 卖盘：价格从低到高 */
    private final ConcurrentSkipListMap<Long, Deque<Order>> asks;

    /** 订单ID → 所在价位 & 买/卖标识（用于撤单快速定位） */
    private final ConcurrentHashMap<String, OrderLocation> orderIndex;

    public OrderBook() {
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        this.asks = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
        this.orderIndex = new ConcurrentHashMap<>();
    }

    // ==================== 添加订单 ====================

    /**
     * 将订单加入订单簿
     *
     * @param order  订单
     * @param isBid  true=买盘，false=卖盘
     */
    public void add(Order order, boolean isBid) {
        long price = order.getPrice();
        ConcurrentSkipListMap<Long, Deque<Order>> book = isBid ? bids : asks;

        book.compute(price, (k, existing) -> {
            Deque<Order> queue = existing;
            if (queue == null) {
                queue = new ConcurrentLinkedDeque<>();
            }
            queue.addLast(order);
            return queue;
        });

        orderIndex.put(order.getOrderId(), new OrderLocation(price, isBid));
    }

    // ==================== 获取最优价 ====================

    /** 获取最优买价（最高价） */
    public Long getBestBidPrice() {
        Map.Entry<Long, Deque<Order>> entry = bids.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /** 获取最优卖价（最低价） */
    public Long getBestAskPrice() {
        Map.Entry<Long, Deque<Order>> entry = asks.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /** 获取指定价位的买盘队列 */
    public Deque<Order> getBidsAtPrice(long price) {
        return bids.get(price);
    }

    /** 获取指定价位的卖盘队列 */
    public Deque<Order> getAsksAtPrice(long price) {
        return asks.get(price);
    }

    // ==================== 取对手盘 ====================

    /**
     * 获取买盘中小于等于指定价格的所有价位的迭代器（从高到低）
     */
    public NavigableMap<Long, Deque<Order>> getBidsUpToPrice(long price) {
        return bids.tailMap(price, true);
    }

    /**
     * 获取卖盘中大于等于指定价格的所有价位的迭代器（从低到高）
     */
    public NavigableMap<Long, Deque<Order>> getAsksUpToPrice(long price) {
        return asks.headMap(price, true);
    }

    /** 获取买盘迭代器（从高到低） */
    public Iterable<Map.Entry<Long, Deque<Order>>> bids() {
        return bids.entrySet();
    }

    /** 获取卖盘迭代器（从低到高） */
    public Iterable<Map.Entry<Long, Deque<Order>>> asks() {
        return asks.entrySet();
    }

    // ==================== 撤单 ====================

    /**
     * 从订单簿中移除指定订单
     *
     * @param orderId 订单ID
     * @return true=移除成功，false=未找到
     */
    public boolean cancelOrder(String orderId) {
        OrderLocation loc = orderIndex.get(orderId);
        if (loc == null) return false;

        ConcurrentSkipListMap<Long, Deque<Order>> book = loc.isBid ? bids : asks;
        Deque<Order> queue = book.get(loc.price);
        if (queue == null) return false;

        boolean removed = queue.removeIf(o -> o.getOrderId().equals(orderId));
        if (removed) {
            orderIndex.remove(orderId);
            // 清理空队列
            if (queue.isEmpty()) {
                book.remove(loc.price, queue);
            }
            log.debug("撤单成功: orderId={}, price={}, side={}",
                    orderId, loc.price, loc.isBid ? "BUY" : "SELL");
        }
        return removed;
    }

    // ==================== 批量处理（清空价格级别） ====================

    /**
     * 移除并返回指定价位的买盘队列
     */
    public Deque<Order> removeBidsAtPrice(long price) {
        Deque<Order> queue = bids.remove(price);
        if (queue != null) {
            queue.forEach(o -> orderIndex.remove(o.getOrderId()));
        }
        return queue;
    }

    /**
     * 移除并返回指定价位的卖盘队列
     */
    public Deque<Order> removeAsksAtPrice(long price) {
        Deque<Order> queue = asks.remove(price);
        if (queue != null) {
            queue.forEach(o -> orderIndex.remove(o.getOrderId()));
        }
        return queue;
    }

    // ==================== 状态查询 ====================

    public int getBidLevelCount() { return bids.size(); }
    public int getAskLevelCount() { return asks.size(); }
    public int getTotalOrderCount() { return orderIndex.size(); }

    public boolean isEmpty() {
        return bids.isEmpty() && asks.isEmpty();
    }

    /**
     * 获取订单所在的价位（用于重建索引）
     */
    public OrderLocation getLocation(String orderId) {
        return orderIndex.get(orderId);
    }

    // ==================== 内部类 ====================

    /**
     * 订单在订单簿中的位置
     */
    public static class OrderLocation {
        public final long price;
        public final boolean isBid;

        public OrderLocation(long price, boolean isBid) {
            this.price = price;
            this.isBid = isBid;
        }
    }
}
