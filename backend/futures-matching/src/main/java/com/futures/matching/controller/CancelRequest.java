package com.futures.matching.controller;

import lombok.Data;

/**
 * 撤单请求 DTO。
 * <p>
 * REST 接口 {@code POST /api/v1/matching/cancel} 的请求体。</p>
 */
@Data
public class CancelRequest {

    /** 合约代码 */
    private String symbol;

    /** 要撤销的订单 ID */
    private String orderId;
}
