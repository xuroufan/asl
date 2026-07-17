package com.futures.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息 VO（前端展示用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long userId;
    private String username;
    private String displayName;
    private String phone;
    private String email;
    private String role;
    private Integer status;       // 0正常 1锁定 2冻结 3销户
    private Integer kycStatus;    // 0未提交 1审核中 2已通过 3已拒绝
    private String realName;
    private String idCardNo;
    private Boolean twoFactorEnabled;
    private String tradingPermissions;
    private Integer maxPositionVolume;
    private String createdAt;
}
