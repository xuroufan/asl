package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件。
 * 订单验证通过后由订单服务发送给撮合引擎。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 合约代码 */
    private String symbol;

    /** 买卖方向 BUY / SELL */
    private String direction;

    /** 订单类型 LIMIT / MARKET / STOP / STOP_LIMIT */
    private String orderType;

    /** 订单价格（市价单为0） */
    private BigDecimal price;

    /** 触发价格（止损/止盈单） */
    private BigDecimal triggerPrice;

    /** 委托数量 */
    private int volume;

    /** 客户端订单号（幂等性） */
    private String clientOrderId;

    /** 订单创建时间 */
    private LocalDateTime createdAt;

    /** 订单来源：API / WEB / MOBILE */
    private String source;

    /** 策略参数（可选，算法单使用） */
    private String strategyParams;
}
