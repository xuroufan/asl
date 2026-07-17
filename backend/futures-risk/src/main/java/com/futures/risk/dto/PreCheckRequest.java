package com.futures.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 前置风控校验请求。
 * <p>包含下单所需的全部校验参数。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCheckRequest {

    /** 用户ID */
    private Long userId;

    /** 合约代码 */
    private String symbol;

    /** 方向：BUY/SELL */
    private String direction;

    /** 手数 */
    private Integer volume;

    /** 价格 */
    private BigDecimal price;

    /** 订单类型：LIMIT/MARKET/STOP */
    private String orderType;

    /** 可用资金（从资金服务预获取，可选） */
    private BigDecimal availableFunds;

    /** 波动率加成（可选，默认 0） */
    private BigDecimal volatility;
}
