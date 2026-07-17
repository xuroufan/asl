package com.futures.matching.controller;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单请求 DTO。
 * <p>
 * REST 接口 {@code POST /api/v1/matching/place} 的请求体。</p>
 */
@Data
public class MatchingRequest {

    /** 合约代码（如 "HSI2309"） */
    private String symbol;

    /** 用户 ID */
    private Long userId;

    /** 买卖方向：BUY / SELL */
    private String direction;

    /** 订单类型：LIMIT / MARKET / STOP / TAKE_PROFIT / FOK / IOC */
    private String orderType;

    /** 价格（限价单必填，市价单忽略） */
    private BigDecimal price;

    /** 数量（手数） */
    private Integer volume;

    /** 止损/止盈触发价（止损单/止盈单必填） */
    private BigDecimal triggerPrice;

    /** 客户端订单号（用于幂等性，可选） */
    private String clientOrderId;
}
