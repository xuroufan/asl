package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录实体。
 * <p>记录每日与交易所/银行的对账结果。</p>
 */
@Data
@TableName("reconciliation")
public class ReconciliationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对账日期 */
    private LocalDate reconciliationDate;

    /** 对账类型：EXCHANGE（交易所）/ BANK（银行） */
    private String reconciliationType;

    /** 总交易/流水笔数 */
    private Integer totalRecords;

    /** 匹配笔数 */
    private Integer matchedRecords;

    /** 未匹配笔数 */
    private Integer unmatchedRecords;

    /** 状态：PENDING / COMPLETED / FAILED */
    private String status;

    /** 对账摘要 */
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime completedTime;

    @TableLogic
    private Integer deleted;
}
