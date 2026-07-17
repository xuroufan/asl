package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算订单明细实体。
 * <p>记录每日结算中每个平仓订单的详细盈亏数据。</p>
 */
@Data
@TableName("settlement_order")
public class SettlementOrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结算ID */
    private Long settlementId;

    /** 用户ID */
    private Long userId;

    /** 订单ID */
    private Long orderId;

    /** 合约代码 */
    private String symbol;

    /** 开仓方向：BUY/SELL */
    private String direction;

    /** 开仓价 */
    private BigDecimal openPrice;

    /** 平仓价 */
    private BigDecimal closePrice;

    /** 平仓数量 */
    private Integer volume;

    /** 平仓盈亏 */
    private BigDecimal pnl;

    /** 手续费 */
    private BigDecimal fee;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
