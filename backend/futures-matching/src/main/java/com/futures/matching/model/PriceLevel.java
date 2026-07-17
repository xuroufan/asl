package com.futures.matching.model;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 价格级别。
 * <p>
 * 同一价格的订单按 FIFO 队列组织。支持添加、删除和批量取消操作。</p>
 */
@Getter
public class PriceLevel {

    /** 价格（原始 BigDecimal） */
    private final java.math.BigDecimal price;

    /** 价格编码 */
    private final long priceEncoded;

    /** 本价格级别所有订单（FIFO 队列） */
    private final Deque<MatchingOrder> orders;

    /** 本价格级别总数量 */
    private long totalVolume;

    public PriceLevel(java.math.BigDecimal price, long priceEncoded) {
        this.price = price;
        this.priceEncoded = priceEncoded;
        this.orders = new ArrayDeque<>();
        this.totalVolume = 0;
    }

    /** 添加订单到队列尾部 */
    public void addOrder(MatchingOrder order) {
        orders.addLast(order);
        totalVolume += order.getVolume();
    }

    /** 从队列头部取出下一单（不移除） */
    public MatchingOrder peekHead() {
        return orders.peekFirst();
    }

    /** 从队列头部取出并移除下一单 */
    public MatchingOrder pollHead() {
        MatchingOrder order = orders.pollFirst();
        if (order != null) {
            totalVolume -= order.getVolume();
        }
        return order;
    }

    /** 队列是否为空 */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /** 订单数量 */
    public int orderCount() {
        return orders.size();
    }
}
