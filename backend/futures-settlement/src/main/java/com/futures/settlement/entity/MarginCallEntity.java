package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 追保通知实体。
 * <p>当客户权益低于维持保证金时，系统自动生成追保通知。</p>
 */
@Data
@TableName("margin_call")
public class MarginCallEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联结算ID */
    private Long settlementId;

    /** 追保类型：INITIAL / MAINTENANCE */
    private String marginCallType;

    /** 追保金额 */
    private BigDecimal requiredAmount;

    /** 当前保证金 */
    private BigDecimal currentMargin;

    /** 当前权益 */
    private BigDecimal currentEquity;

    /** 状态：PENDING / SENT / RESOLVED */
    private String status;

    /** 发送时间 */
    private LocalDateTime sentTime;

    /** 解决时间 */
    private LocalDateTime resolvedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String remarks;

    @TableLogic
    private Integer deleted;
}
