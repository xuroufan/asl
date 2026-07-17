package com.futures.account.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOTPUtil 单元测试。
 * <p>验证密钥生成、验证码生成和验证逻辑的正确性。</p>
 */
class TOTPUtilTest {

    @Test
    void testGenerateSecret_shouldReturnBase32String() {
        String secret = TOTPUtil.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
        // Base32 编码只包含 A-Z 和 2-7
        assertTrue(secret.matches("[A-Z2-7]+"));
    }

    @Test
    void testGenerateSecret_shouldBeRandom() {
        String secret1 = TOTPUtil.generateSecret();
        String secret2 = TOTPUtil.generateSecret();
        assertNotEquals(secret1, secret2);
    }

    @Test
    void testGenerateCode_shouldReturnSixDigits() {
        String secret = TOTPUtil.generateSecret();
        String code = TOTPUtil.generateCode(secret);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testGenerateCode_withSameSecret_shouldReturnSameCodeForSameTimeStep() {
        String secret = TOTPUtil.generateSecret();
        long timeStep = 12345678L;
        String code1 = TOTPUtil.generateCode(secret, timeStep);
        String code2 = TOTPUtil.generateCode(secret, timeStep);
        assertEquals(code1, code2);
    }

    @Test
    void testGenerateCode_withDifferentTimeStep_shouldReturnDifferentCode() {
        String secret = TOTPUtil.generateSecret();
        String code1 = TOTPUtil.generateCode(secret, 1000L);
        String code2 = TOTPUtil.generateCode(secret, 1001L);
        assertNotEquals(code1, code2);
    }

    @Test
    void testVerifyCode_withValidCode_shouldReturnTrue() {
        String secret = TOTPUtil.generateSecret();
        String code = TOTPUtil.generateCode(secret);
        assertTrue(TOTPUtil.verifyCode(secret, code));
    }

    @Test
    void testVerifyCode_withInvalidCode_shouldReturnFalse() {
        String secret = TOTPUtil.generateSecret();
        assertFalse(TOTPUtil.verifyCode(secret, "000000"));
    }

    @Test
    void testVerifyCode_withEmptyCode_shouldReturnFalse() {
        String secret = TOTPUtil.generateSecret();
        assertFalse(TOTPUtil.verifyCode(secret, ""));
    }

    @Test
    void testGenerateQRUrl_shouldContainCorrectIssuerAndAccount() {
        String secret = TOTPUtil.generateSecret();
        String url = TOTPUtil.generateQRUrl("Futures", "testuser", secret);
        assertNotNull(url);
        assertTrue(url.startsWith("otpauth://totp/"));
        assertTrue(url.contains("Futures:testuser"));
        assertTrue(url.contains("secret=" + secret));
        assertTrue(url.contains("issuer=Futures"));
    }
}
