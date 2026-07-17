package com.futures.matching.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成交记录
 */
@Data
@Builder
@AllArgsConstructor
public class MatchResult {
    private String takerOrderId;      // 主动单ID
    private String makerOrderId;      // 被动单ID（对手单）
    private long price;               // 成交价格
    private int volume;               // 成交数量
    private long timestamp;           // 成交时间（纳秒）
    private String symbol;            // 合约代码

    public BigDecimal getPriceAsBigDecimal() {
        return BigDecimal.valueOf(price);
    }
}
