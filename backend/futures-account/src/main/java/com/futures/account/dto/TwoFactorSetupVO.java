package com.futures.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 双因素认证设置响应 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupVO {

    /** TOTP 密钥（Base32） */
    private String secret;

    /** Google Authenticator 兼容的 otpauth URL */
    private String qrCodeUrl;

    /** 是否已启用 */
    private boolean enabled;
}
