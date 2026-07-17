package com.futures.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.futures.account.entity.AccountEntity;
import com.futures.account.entity.UserEntity;
import com.futures.account.mapper.AccountMapper;
import com.futures.account.mapper.UserMapper;
import com.futures.account.util.TOTPUtil;
import com.futures.common.dto.LoginRequest;
import com.futures.common.dto.LoginResponse;
import com.futures.common.exception.BizException;
import com.futures.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务。
 * <p>处理用户注册、登录（BCrypt + 2FA）、Token 刷新、登录失败锁定等功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户登录。
     * <ol>
     *   <li>BCrypt 验证密码</li>
     *   <li>登录失败 5 次锁定 15 分钟</li>
     *   <li>若已开启 2FA，返回 need2FA=true，前端需弹出 2FA 验证框</li>
     * </ol>
     *
     * @param request 登录请求
     * @return 登录响应（含 Token 和 2FA 标识）
     */
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.getUsername()));

        if (user == null) {
            throw BizException.badRequest("用户名或密码错误");
        }

        // 检查账户锁定
        if (user.getStatus() != null && user.getStatus() == 1) {
            if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
                long minutes = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes();
                throw BizException.badRequest("账户已被锁定，请 " + minutes + " 分钟后重试");
            }
            // 锁定时间已过，自动解锁
            user.setStatus(0);
            user.setFailCount(0);
            userMapper.updateById(user);
        }

        // BCrypt 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleLoginFail(user);
            throw BizException.badRequest("用户名或密码错误");
        }

        // 登录成功，重置失败次数
        user.setFailCount(0);
        userMapper.updateById(user);

        // 生成 Token
        // 使用 RS256 签名，携带角色和权限信息
        String roles = user.getRole() != null ? user.getRole() : "USER";
        String permissions = user.getTradingPermissions() != null ? user.getTradingPermissions() : "";
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = JwtUtil.generateRefreshToken(user.getId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .build();

        boolean need2FA = user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
        log.info("用户 {} 登录成功，角色={}", user.getUsername(), roles);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .need2FA(need2FA)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 验证双因素认证（2FA）。
     * <p>用户完成密码登录后，若开启了 2FA，需调用此接口验证 TOTP 验证码。</p>
     *
     * @param userId 用户 ID
     * @param code   用户输入的 6 位 TOTP 验证码
     * @return 新的 accessToken（2FA 验证通过后颁发完整 Token）
     */
    public LoginResponse verifyTwoFactor(Long userId, String code) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (user.getTwoFactorSecret() == null || !user.getTwoFactorEnabled()) {
            throw BizException.badRequest("用户未开启双因素认证");
        }
        if (!TOTPUtil.verifyCode(user.getTwoFactorSecret(), code)) {
            throw BizException.badRequest("验证码错误，请重试");
        }
        log.info("用户 {} 2FA 验证通过", user.getUsername());

        // 2FA 通过后也使用 RS256 签名
        String roles = user.getRole() != null ? user.getRole() : "USER";
        String permissions = user.getTradingPermissions() != null ? user.getTradingPermissions() : "";
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId()).username(user.getUsername())
                .displayName(user.getDisplayName()).role(user.getRole()).build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .need2FA(false)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 刷新 Token。
     *
     * @param refreshToken 刷新令牌
     * @return 新的 Token 对
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!JwtUtil.validateToken(refreshToken) || !JwtUtil.isRefreshToken(refreshToken)) {
            throw BizException.unauthorized("RefreshToken 无效或已过期");
        }
        Long userId = JwtUtil.getUserId(refreshToken);
        String username = JwtUtil.getUsername(refreshToken);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }

        // Refresh Token 时使用原始角色和权限
        String roles = user.getRole() != null ? user.getRole() : "USER";
        String permissions = user.getTradingPermissions() != null ? user.getTradingPermissions() : "";
        String newAccessToken = JwtUtil.generateAccessToken(userId, username, roles, permissions);
        String newRefreshToken = JwtUtil.generateRefreshToken(userId);

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId()).username(user.getUsername())
                .displayName(user.getDisplayName()).role(user.getRole()).build();

        return LoginResponse.builder()
                .accessToken(newAccessToken).refreshToken(newRefreshToken)
                .need2FA(false).userInfo(userInfo).build();
    }

    /**
     * 用户注册。
     * <p>使用 BCrypt 加密密码存储，同时自动创建默认期货账户。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录响应（含 Token）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(String username, String password) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username));
        if (count > 0) {
            throw BizException.badRequest("用户名已存在");
        }

        // 创建用户 — 密码使用 BCrypt 加密
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(username);
        user.setRole("USER");
        user.setStatus(0);
        user.setFailCount(0);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 创建账户
        AccountEntity account = new AccountEntity();
        account.setUserId(user.getId());
        account.setCashBalance(new BigDecimal("1000000.00"));
        account.setEquityWithLoan(new BigDecimal("1000000.00"));
        account.setInitialMargin(BigDecimal.ZERO);
        account.setMaintenanceMargin(BigDecimal.ZERO);
        account.setAvailableFunds(new BigDecimal("1000000.00"));
        account.setDailyPnl(BigDecimal.ZERO);
        account.setDailyLossLimit(new BigDecimal("20000.00"));
        account.setTotalPnl(BigDecimal.ZERO);
        account.setCreatedAt(LocalDateTime.now());
        accountMapper.insert(account);

        log.info("用户 {} 注册成功，初始资金 1,000,000.00", username);

        // 生成 Token
        // 注册时使用 RS256 签名，携带角色和权限
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), "USER", "");
        String refreshToken = JwtUtil.generateRefreshToken(user.getId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId()).username(user.getUsername())
                .displayName(user.getDisplayName()).role(user.getRole()).build();

        return LoginResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken)
                .need2FA(false).userInfo(userInfo).build();
    }

    /**
     * 处理登录失败逻辑。
     * <p>失败 5 次后锁定账户 15 分钟。</p>
     */
    private void handleLoginFail(UserEntity user) {
        int failCount = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
        user.setFailCount(failCount);
        if (failCount >= 5) {
            user.setStatus(1);
            user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("用户 {} 登录失败 5 次，锁定至 {}", user.getUsername(), user.getLockUntil());
        }
        userMapper.updateById(user);
    }
}
