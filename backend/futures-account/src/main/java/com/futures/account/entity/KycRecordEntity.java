package com.futures.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KYC 审核记录实体。
 * <p>记录每次 KYC 提交和审核结果，支持多次提交审核。</p>
 */
@Data
@TableName("kyc_record")
public class KycRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCardNo;

    /** 身份证正面照URL */
    private String idCardFrontUrl;

    /** 身份证背面照URL */
    private String idCardBackUrl;

    /** 审核状态：1-审核中, 2-已通过, 3-已拒绝 */
    private Integer status;

    /** 审核意见（拒绝原因） */
    private String remark;

    /** 审核人 */
    private String reviewer;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @TableLogic
    private Integer deleted;
}
