package com.futures.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.futures.account.dto.TwoFactorSetupVO;
import com.futures.account.dto.UserProfileVO;
import com.futures.account.entity.UserEntity;
import com.futures.account.mapper.UserMapper;
import com.futures.account.util.TOTPUtil;
import com.futures.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 用户管理服务。
 * <p>处理用户资料、2FA 设置、密码修改、账户冻结等操作。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 获取用户资料。
     *
     * @param userId 用户 ID
     * @return 用户资料 VO
     */
    public UserProfileVO getProfile(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .kycStatus(user.getKycStatus())
                .realName(user.getRealName())
                .idCardNo(user.getIdCardNo())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .tradingPermissions(user.getTradingPermissions())
                .maxPositionVolume(user.getMaxPositionVolume())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 更新用户资料（手机号/邮箱）。
     *
     * @param userId 用户 ID
     * @param updates 更新字段
     */
    public void updateProfile(Long userId, Map<String, String> updates) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }
        if (updates.containsKey("displayName")) {
            user.setDisplayName(updates.get("displayName"));
        }
        userMapper.updateById(user);
        log.info("用户 {} 更新资料", userId);
    }

    /**
     * 设置双因素认证（2FA） — 生成 TOTP 密钥。
     * <p>返回密钥和 otpauth URL，用户需在 Authenticator 应用中绑定后调用 enableTwoFactor 完成启用。</p>
     *
     * @param userId 用户 ID
     * @return 2FA 设置信息（密钥 + 二维码 URL）
     */
    public TwoFactorSetupVO setupTwoFactor(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
            throw BizException.badRequest("双因素认证已启用，请先关闭后再重新设置");
        }

        String secret = TOTPUtil.generateSecret();
        String qrUrl = TOTPUtil.generateQRUrl("Futures", user.getUsername(), secret);

        // 暂存密钥（尚未启用）
        user.setTwoFactorSecret(secret);
        userMapper.updateById(user);

        log.info("用户 {} 生成 2FA 密钥", userId);

        return TwoFactorSetupVO.builder()
                .secret(secret)
                .qrCodeUrl(qrUrl)
                .enabled(false)
                .build();
    }

    /**
     * 启用双因素认证（2FA）。
     * <p>验证用户输入的 TOTP 验证码正确后，正式启用 2FA。</p>
     *
     * @param userId 用户 ID
     * @param code   用户从 Authenticator 获取的 6 位验证码
     */
    public void enableTwoFactor(Long userId, String code) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (user.getTwoFactorSecret() == null) {
            throw BizException.badRequest("请先执行 2FA 设置生成密钥");
        }
        if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
            throw BizException.badRequest("双因素认证已启用");
        }

        if (!TOTPUtil.verifyCode(user.getTwoFactorSecret(), code)) {
            throw BizException.badRequest("验证码错误，请确认 Authenticator 应用中的密钥正确");
        }

        user.setTwoFactorEnabled(true);
        userMapper.updateById(user);
        log.info("用户 {} 启用 2FA 成功", userId);
    }

    /**
     * 关闭双因素认证（2FA）。
     *
     * @param userId 用户 ID
     */
    public void disableTwoFactor(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userMapper.updateById(user);
        log.info("用户 {} 关闭 2FA", userId);
    }

    /**
     * 修改密码。
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BizException.badRequest("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("用户 {} 修改密码成功", userId);
    }

    /**
     * 冻结账户（管理员操作）。
     *
     * @param userId 用户 ID
     */
    public void freezeAccount(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setStatus(2); // 冻结
        userMapper.updateById(user);
        log.warn("用户 {} 账户被冻结", userId);
    }

    /**
     * 解冻账户（管理员操作）。
     *
     * @param userId 用户 ID
     */
    public void unfreezeAccount(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setStatus(0); // 正常
        userMapper.updateById(user);
        log.info("用户 {} 账户已解冻", userId);
    }


    /**
     * 获取用户的交易权限列表（逗号分隔）。
     *
     * @param userId 用户 ID
     * @return 权限字符串
     */
    public String getUserPermissions(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user.getTradingPermissions() != null ? user.getTradingPermissions() : "";
    }

    /**
     * 设置用户的交易权限（管理员操作）。
     *
     * @param userId 用户 ID
     * @param permissions 逗号分隔的权限编码列表
     */
    public void setUserPermissions(Long userId, String permissions) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setTradingPermissions(permissions);
        userMapper.updateById(user);
        log.info("管理员设置用户 {} 权限: {}", userId, permissions);
    }

    /**
     * 更新用户角色（管理员操作）。
     *
     * @param userId 用户 ID
     * @param role 角色名（USER/VIP/ADMIN）
     */
    public void updateUserRole(Long userId, String role) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (!java.util.Set.of("USER", "VIP", "ADMIN").contains(role)) {
            throw BizException.badRequest("角色无效，支持: USER, VIP, ADMIN");
        }
        user.setRole(role);
        userMapper.updateById(user);
        log.info("管理员设置用户 {} 角色: {}", userId, role);
    }

}