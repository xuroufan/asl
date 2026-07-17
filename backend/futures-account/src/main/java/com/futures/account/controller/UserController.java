package com.futures.account.controller;

import com.futures.account.dto.TwoFactorSetupVO;
import com.futures.account.dto.UserProfileVO;
import com.futures.account.service.UserService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器。
 * <p>处理用户资料查询/更新、2FA 绑定、密码修改、账户冻结/解冻。</p>
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户资料。
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    /**
     * 更新用户资料（手机号/邮箱/显示名）。
     */
    @PutMapping("/profile")
    public Result<String> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> updates) {
        userService.updateProfile(userId, updates);
        return Result.success("资料已更新");
    }

    /**
     * 设置双因素认证（2FA）— 生成 TOTP 密钥和二维码 URL。
     * <p>前端展示二维码让用户扫描绑定，绑定后调用 enableTwoFactor 完成启用。</p>
     */
    @PostMapping("/2fa/setup")
    public Result<TwoFactorSetupVO> setupTwoFactor(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.setupTwoFactor(userId));
    }

    /**
     * 启用双因素认证（2FA）— 验证用户提交的 TOTP 验证码。
     */
    @PostMapping("/2fa/enable")
    public Result<String> enableTwoFactor(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        userService.enableTwoFactor(userId, body.get("code"));
        return Result.success("双因素认证已启用");
    }

    /**
     * 关闭双因素认证（2FA）。
     */
    @PostMapping("/2fa/disable")
    public Result<String> disableTwoFactor(@RequestHeader("X-User-Id") Long userId) {
        userService.disableTwoFactor(userId);
        return Result.success("双因素认证已关闭");
    }

    /**
     * 修改登录密码。
     */
    @PostMapping("/password/change")
    public Result<String> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success("密码已修改");
    }

    /**
     * 管理员 — 冻结用户账户。
     */
    @PostMapping("/{userId}/freeze")
    public Result<String> freezeAccount(@PathVariable Long userId) {
        userService.freezeAccount(userId);
        return Result.success("账户已冻结");
    }

    /**
     * 管理员 — 解冻用户账户。
     */
    @PostMapping("/{userId}/unfreeze")
    public Result<String> unfreezeAccount(@PathVariable Long userId) {
        userService.unfreezeAccount(userId);
        return Result.success("账户已解冻");
    }
}
