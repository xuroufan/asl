package com.futures.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 * <p>涵盖认证、KYC、2FA、权限和账户状态管理。</p>
 */
@Data
@TableName("t_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 显示名称 */
    private String displayName;

    /** 手机号（唯一） */
    private String phone;

    /** 邮箱（唯一） */
    private String email;

    /** 角色：USER-普通用户, VIP-VIP用户, ADMIN-管理员 */
    private String role;

    /** 账户状态：0-正常, 1-锁定, 2-冻结, 3-销户 */
    private Integer status;

    // ==================== KYC 字段 ====================

    /** KYC状态：0-未提交, 1-审核中, 2-已通过, 3-已拒绝 */
    private Integer kycStatus;

    /** 真实姓名（KYC 审核通过后填写） */
    private String realName;

    /** 身份证号 */
    private String idCardNo;

    /** 身份证正面照URL */
    private String idCardFrontUrl;

    /** 身份证背面照URL */
    private String idCardBackUrl;

    // ==================== 2FA 字段 ====================

    /** 是否启用双因素认证 */
    private Boolean twoFactorEnabled;

    /** TOTP 密钥（Base32 编码） */
    private String twoFactorSecret;

    // ==================== 交易权限字段 ====================

    /** 交易权限（逗号分隔：ES,GC,CL,NQ 或 * 表示全部） */
    private String tradingPermissions;

    /** 单合约最大持仓手数 */
    private Integer maxPositionVolume;

    // ==================== 登录安全字段 ====================

    /** 登录失败次数 */
    private Integer failCount;

    /** 锁定截止时间 */
    private LocalDateTime lockUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
