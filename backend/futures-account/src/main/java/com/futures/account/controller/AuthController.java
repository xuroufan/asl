package com.futures.account.controller;

import com.futures.account.service.AuthService;
import com.futures.common.dto.LoginRequest;
import com.futures.common.dto.LoginResponse;
import com.futures.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器。
 * <p>处理登录、注册、Token 刷新、2FA 验证等认证相关请求。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录（密码 + 图形验证码）。
     * <p>若用户已开启 2FA，返回 need2FA=true，前端需弹出 2FA 验证框。</p>
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody Map<String, String> body) {
        return Result.success(authService.register(body.get("username"), body.get("password")));
    }

    /**
     * 刷新 Token。
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return Result.success(authService.refreshToken(body.get("refreshToken")));
    }

    /**
     * 双因素认证（2FA）验证。
     * <p>用户完成密码登录后，若 need2FA=true，调用此接口验证 TOTP 验证码。</p>
     */
    @PostMapping("/verify-2fa")
    public Result<LoginResponse> verifyTwoFactor(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String code = body.get("code").toString();
        return Result.success(authService.verifyTwoFactor(userId, code));
    }

    /**
     * 获取图形验证码（演示用，返回固定值）。
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.success(Map.of("captcha", "1234"));
    }
}
