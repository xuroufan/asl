package com.futures.common.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算完成事件。
 * 清结算服务每日收盘后发送给账户/通知服务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDoneEvent {

    /** 用户ID */
    private String userId;

    /** 结算日期 */
    private LocalDate settlementDate;

    /** 结算前权益 */
    private BigDecimal openingEquity;

    /** 结算后权益 */
    private BigDecimal closingEquity;

    /** 当日盈亏 */
    private BigDecimal dailyProfit;

    /** 手续费 */
    private BigDecimal fee;

    /** 结算后保证金 */
    private BigDecimal margin;

    /** 结算状态 SUCCESS / MARGIN_CALL / LIQUIDATION */
    private String status;
}
