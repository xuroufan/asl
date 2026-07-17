package com.futures.account.service;

import com.futures.account.entity.AccountEntity;
import com.futures.account.entity.UserEntity;
import com.futures.account.mapper.AccountMapper;
import com.futures.account.mapper.UserMapper;
import com.futures.account.util.TOTPUtil;
import com.futures.common.dto.LoginRequest;
import com.futures.common.dto.LoginResponse;
import com.futures.common.exception.BizException;
import com.futures.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试。
 * <p>验证 BCrypt 加密、2FA 验证、登录失败锁定等逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AccountMapper accountMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, accountMapper, new BCryptPasswordEncoder(10));
    }

    @Test
    void register_shouldEncryptPasswordWithBCrypt() {
        String username = "testuser";
        String password = "password123";

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });
        when(accountMapper.insert(any())).thenReturn(1);

        LoginResponse response = authService.register(username, password);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertFalse(response.isNeed2FA());

        // 验证密码被 BCrypt 加密存储
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        verify(userMapper).insert(argThat(user ->
                encoder.matches(password, user.getPassword())
        ));
    }

    @Test
    void register_withDuplicateUsername_shouldThrowException() {
        String username = "existingUser";
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BizException.class, () ->
                authService.register(username, "password123"));
    }

    @Test
    void login_withCorrectPassword_shouldSucceed() {
        String password = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode(password);

        UserEntity user = createTestUser(1L, "testuser", encodedPassword, false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(password);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertFalse(response.isNeed2FA());
    }

    @Test
    void login_withWrongPassword_shouldThrowException() {
        String encodedPassword = new BCryptPasswordEncoder().encode("correctPassword");

        UserEntity user = createTestUser(1L, "testuser", encodedPassword, false);
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");

        assertThrows(BizException.class, () -> authService.login(request));
    }

    @Test
    void login_withFailCount5_shouldLockAccount() {
        String encodedPassword = new BCryptPasswordEncoder().encode("correctPassword");

        UserEntity user = createTestUser(1L, "testuser", encodedPassword, false);
        user.setFailCount(4); // 已失败 4 次
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");

        assertThrows(BizException.class, () -> authService.login(request));

        // 验证账户被锁定
        assertNotNull(user.getLockUntil());
        assertTrue(user.getLockUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void login_with2FAEnabled_shouldReturnNeed2FA() {
        String password = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode(password);

        UserEntity user = createTestUser(1L, "testuser", encodedPassword, true);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(password);

        LoginResponse response = authService.login(request);

        assertTrue(response.isNeed2FA());
    }

    @Test
    void verifyTwoFactor_withValidCode_shouldSucceed() {
        String secret = TOTPUtil.generateSecret();
        UserEntity user = createTestUser(1L, "testuser", "encoded", true);
        user.setTwoFactorSecret(secret);

        when(userMapper.selectById(1L)).thenReturn(user);

        String code = TOTPUtil.generateCode(secret);
        LoginResponse response = authService.verifyTwoFactor(1L, code);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
    }

    @Test
    void verifyTwoFactor_withInvalidCode_shouldThrowException() {
        String secret = TOTPUtil.generateSecret();
        UserEntity user = createTestUser(1L, "testuser", "encoded", true);
        user.setTwoFactorSecret(secret);

        when(userMapper.selectById(1L)).thenReturn(user);

        assertThrows(BizException.class, () ->
                authService.verifyTwoFactor(1L, "000000"));
    }

    @Test
    void verifyTwoFactor_with2FANotEnabled_shouldThrowException() {
        UserEntity user = createTestUser(1L, "testuser", "encoded", false);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThrows(BizException.class, () ->
                authService.verifyTwoFactor(1L, "123456"));
    }

    /** 创建测试用户 */
    private UserEntity createTestUser(Long id, String username, String password, boolean twoFactorEnabled) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setDisplayName(username);
        user.setRole("USER");
        user.setStatus(0);
        user.setTwoFactorEnabled(twoFactorEnabled);
        user.setTwoFactorSecret(twoFactorEnabled ? TOTPUtil.generateSecret() : null);
        user.setFailCount(0);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
