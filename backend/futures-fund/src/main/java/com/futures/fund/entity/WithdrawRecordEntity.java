package com.futures.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出金记录实体。
 * <p>记录用户提现申请及审核状态。</p>
 */
@Data
@TableName("withdraw_record")
public class WithdrawRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 出金金额 */
    private BigDecimal amount;

    /** 银行卡信息 */
    private String bankInfo;

    /** 状态：0=待审核,1=已通过,2=已拒绝,3=已完成 */
    private Integer status;

    /** 审核人 */
    private String reviewer;

    /** 审核备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime reviewTime;

    @TableLogic
    private Integer deleted;
}
