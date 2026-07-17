package com.futures.fund.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 冻结保证金响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeResponse {

    /** 是否成功 */
    private boolean success;

    /** 冻结后的可用资金 */
    private BigDecimal availableAfter;

    /** 冻结后的保证金总和 */
    private BigDecimal frozenAfter;

    /** 提示/错误消息 */
    private String message;
}
