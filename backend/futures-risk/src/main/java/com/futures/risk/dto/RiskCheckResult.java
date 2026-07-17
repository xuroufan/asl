package com.futures.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 前置风控校验结果。
 * <p>包含是否通过、失败原因、以及校验中计算出的保证金金额。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResult {

    /** 是否通过所有校验 */
    private boolean passed;

    /** 校验失败原因（passed=true 时为空） */
    private String reason;

    /** 错误码（通过时为 200） */
    private int code;

    /** 计算出的开仓保证金 */
    private BigDecimal requiredMargin;

    /** 当前可用资金 */
    private BigDecimal availableFunds;

    /** 当前风险度（仅校验通过时返回） */
    private BigDecimal riskRatio;
}
