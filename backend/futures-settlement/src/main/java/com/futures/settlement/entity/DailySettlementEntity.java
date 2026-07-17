package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日终结算实体。
 * <p>记录每个用户每日的结算数据，包括持仓盈亏、平仓盈亏、手续费、资金变动等。</p>
 */
@Data
@TableName("daily_settlement")
public class DailySettlementEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 结算日期 */
    private LocalDate settlementDate;

    /** 期初权益 */
    private BigDecimal beginEquity;

    /** 期末权益 */
    private BigDecimal endEquity;

    /** 平仓盈亏 */
    private BigDecimal realizedPnl;

    /** 持仓浮动盈亏（未实现） */
    private BigDecimal unrealizedPnl;

    /** 总盈亏 = realizedPnl + unrealizedPnl */
    private BigDecimal totalPnl;

    /** 手续费 */
    private BigDecimal fee;

    /** 出入金净额 */
    private BigDecimal netDeposit;

/** 期初占用保证金 */
    private BigDecimal openingMargin;

    /** 期末占用保证金 */
    private BigDecimal closingMargin;

    /** 维持保证金 */
    private BigDecimal maintenanceMargin;

    /** 追保金额 */
    private BigDecimal marginCallAmount;

    /** 结算价 */
    private BigDecimal settlementPrice;

    /** 结算状态：PENDING / COMPLETED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime settledTime;

    @TableLogic
    private Integer deleted;
}
