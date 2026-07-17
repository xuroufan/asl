package com.futures.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水实体 — t_fund_flow 表
 */
@Data
@TableName("t_fund_flow")
public class FundFlowEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 关联订单ID（交易产生时关联） */
    private String orderId;

    /** 流水类型 0=入金 1=出金 2=冻结 3=解冻 4=扣款 5=入账 */
    private Integer flowType;

    /** 变动金额 */
    private BigDecimal amount;

    /** 变动前余额 */
    private BigDecimal beforeBalance;

    /** 变动后余额 */
    private BigDecimal afterBalance;

    /** 变动前可用 */
    private BigDecimal beforeAvailable;

    /** 变动后可用 */
    private BigDecimal afterAvailable;

    /** 变动前冻结 */
    private BigDecimal beforeFrozen;

    /** 变动后冻结 */
    private BigDecimal afterFrozen;

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
