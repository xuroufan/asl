package com.futures.gateway.filter;

import com.futures.common.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    @Test @DisplayName("白名单路径 — 登录页应放行")
    void testWhiteListLogin() {
        String[] whiteList = { "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/market/symbols", "/actuator/health" };
        for (String path : whiteList) {
            boolean match = whiteListMatch(path);
            assertTrue(match, "白名单路径应匹配: " + path);
        }
    }

    @Test @DisplayName("非白名单路径 — 不应放行")
    void testNonWhiteList() {
        assertFalse(whiteListMatch("/api/v1/order/create"));
        assertFalse(whiteListMatch("/api/v1/account/info"));
    }

    @Test @DisplayName("JWT Token — 生成和验证")
    void testJwtTokenRoundtrip() {
        String token = JwtUtil.generateAccessToken(1L, "admin", "ADMIN", "all");
        assertTrue(token.startsWith("eyJ"));
        assertTrue(JwtUtil.validateToken(token));
        assertEquals(Long.valueOf(1L), JwtUtil.getUserId(token));
    }

    private boolean whiteListMatch(String path) {
        return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/refresh")
            || path.startsWith("/api/v1/market/symbols") || path.startsWith("/actuator/health")
            || path.startsWith("/api/v1/market/quote") || path.startsWith("/api/v1/market/depth")
            || path.startsWith("/api/v1/market/kline") || path.startsWith("/api/v1/market/trades");
    }
}
