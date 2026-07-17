package com.futures.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 强平记录 — t_liquidation_record 表
 */
@Data
@TableName("t_liquidation_record")
public class LiquidationRecordEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 合约 */
    private String symbol;

    /** 强平方向 */
    private String direction;

    /** 强平手数 */
    private Integer volume;

    /** 强平价格 */
    private BigDecimal liquidationPrice;

    /** 触发时风险度 */
    private BigDecimal riskRatio;

    /** 触发原因 */
    private String reason;

    /** 状态 0=待执行 1=已执行 2=失败 */
    private Integer status;

    /** 关联订单ID */
    private String orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
