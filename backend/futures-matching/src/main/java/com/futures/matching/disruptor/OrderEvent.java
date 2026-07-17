package com.futures.matching.disruptor;

import com.futures.matching.model.Order;
import lombok.Data;

/**
 * Disruptor 事件 — 订单输入事件
 * <p>
 * 支持三种操作：
 * <ul>
 *   <li>新订单：{@code order != null}</li>
 *   <li>撤单：{@code cancelOperation = true, cancelOrderId != null}</li>
 * </ul>
 */
@Data
public class OrderEvent {
    private String symbol;
    private Order order;
    private boolean cancelOperation;
    private String cancelOrderId;
    private boolean syncMode;
    private Object syncResult;
}
