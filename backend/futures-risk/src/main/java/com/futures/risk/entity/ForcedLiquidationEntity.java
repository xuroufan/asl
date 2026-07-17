package com.futures.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 强平记录实体（t_forced_liquidation）。
 * <p>记录自动强平操作：时间、品种、手数、价格、原因等。</p>
 */
@Data
@TableName("forced_liquidation")
public class ForcedLiquidationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 合约代码 */
    private String symbol;

    /** 强平方向：BUY/SELL */
    private String direction;

    /** 强平手数 */
    private Integer volume;

    /** 强平价格 */
    private BigDecimal price;

    /** 强平前风险度 */
    private BigDecimal riskRatioBefore;

    /** 强平后风险度 */
    private BigDecimal riskRatioAfter;

    /** 强平原因 */
    private String reason;

    /** 强平状态：PENDING-待执行，EXECUTED-已执行，FAILED-失败 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime executedTime;

    @TableLogic
    private Integer deleted;
}
