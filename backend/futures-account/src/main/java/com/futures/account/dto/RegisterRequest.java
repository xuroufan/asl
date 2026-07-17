package com.futures.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度6-128字符")
    private String password;

    /** 手机号（可选） */
    private String phone;

    /** 邮箱（可选） */
    private String email;

    /** 验证码 */
    private String captcha;
}
