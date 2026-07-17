package com.futures.order.service;

import com.futures.order.entity.OrderEntity;

import java.math.BigDecimal;

/**
 * TCC 分布式事务服务接口。
 * 下单流程涉及三个微服务（资金、风控、订单）的 Try/Confirm/Cancel 操作。
 */
public interface OrderTccService {

    /**
     * TCC-Try: 下单前的准备操作。
     * 校验风控 → 冻结保证金 → 检查日内亏损限额。
     */
    void tryPlace(OrderEntity order, BigDecimal price);

    /**
     * TCC-Confirm: Try 成功后提交。
     */
    void confirm(OrderEntity order);

    /**
     * TCC-Cancel: 回滚，解冻保证金。
     */
    void cancel(OrderEntity order);
}
