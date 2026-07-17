package com.futures.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    @DisplayName("生成 accessToken — 有效且非空")
    void testGenerateAccessToken_ReturnsValidToken() {
        String token = JwtUtil.generateAccessToken(1L, "testuser", "USER,VIP", "order:create,order:view");
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("生成的 token 能被 validateToken 验证通过")
    void testGeneratedToken_ValidatesSuccessfully() {
        String token = JwtUtil.generateAccessToken(1L, "testuser");
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("parseToken 能正确解析 token 中的字段")
    void testParseToken_ExtractsClaims() {
        String token = JwtUtil.generateAccessToken(42L, "alice", "ADMIN", "all");
        Claims claims = JwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals("42", claims.getSubject());
        assertEquals("alice", claims.get("username"));
        assertEquals("access", claims.get("type"));
        assertEquals("ADMIN", claims.get("roles"));
    }

    @Test
    @DisplayName("getUserId 提取正确的用户ID")
    void testGetUserId_ReturnsCorrectId() {
        String token = JwtUtil.generateAccessToken(99L, "bob");
        assertEquals(Long.valueOf(99L), JwtUtil.getUserId(token));
    }

    @Test
    @DisplayName("getUsername 提取正确的用户名")
    void testGetUsername_ReturnsCorrectName() {
        String token = JwtUtil.generateAccessToken(1L, "charlie");
        assertEquals("charlie", JwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("isRefreshToken 正确区分 refreshToken")
    void testIsRefreshToken_IdentifiesCorrectly() {
        String accessToken = JwtUtil.generateAccessToken(1L, "user");
        String refreshToken = JwtUtil.generateRefreshToken(1L);
        assertFalse(JwtUtil.isRefreshToken(accessToken));
        assertTrue(JwtUtil.isRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("generateRefreshToken 返回有效refreshToken")
    void testGenerateRefreshToken_Valid() {
        String token = JwtUtil.generateRefreshToken(1L);
        assertNotNull(token);
        assertTrue(JwtUtil.validateToken(token));
        assertTrue(JwtUtil.isRefreshToken(token));
    }
}
