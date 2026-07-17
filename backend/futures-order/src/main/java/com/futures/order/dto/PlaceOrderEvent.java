package com.futures.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 订单创建事件 — MQ 消息体
 * 发送给撮合引擎进行撮合
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderEvent {

    private String orderId;
    private Long userId;
    private String symbol;
    private String direction;       // BUY / SELL
    private String orderType;       // LIMIT / MARKET / STOP
    private BigDecimal price;
    private Integer volume;
    private BigDecimal stopPrice;
}
