package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异明细实体。
 * <p>记录对账过程中发现的每一笔差异详情。</p>
 */
@Data
@TableName("reconciliation_diff")
public class ReconciliationDiffEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对账记录ID */
    private Long reconciliationId;

    /** 差异类型：MISSING / MISMATCH / EXTRA */
    private String diffType;

    /** 我方记录标识 */
    private String ourRecordId;

    /** 对方记录标识 */
    private String theirRecordId;

    /** 我方金额 */
    private BigDecimal ourAmount;

    /** 对方金额 */
    private BigDecimal theirAmount;

    /** 金额差异 */
    private BigDecimal amountDiff;

    /** 差异状态：PENDING / RESOLVED */
    private String status;

    /** 备注 */
    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
