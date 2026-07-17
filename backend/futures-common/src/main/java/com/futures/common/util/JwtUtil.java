package com.futures.common.util;

import com.futures.common.security.RSAKeyProvider;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

/**
 * JWT 工具类（RS256 非对称签名）。
 * <p>
 * 使用 RSAKeyProvider 管理密钥对，支持 accessToken / refreshToken 的生成、验证和解析。
 * 签名算法：RS256（非对称加密，私钥签名、公钥验证 — 网关只需持有公钥即可验证）。
 * </p>
 *
 * 安全规范：
 * <ul>
 *   <li>accessToken 有效期：30 分钟</li>
 *   <li>refreshToken 有效期：7 天</li>
 *   <li>RS256 签名（非对称，私钥签名、公钥验证）</li>
 *   <li>Payload 包含 sub（用户ID）、username、type（access/refresh）、roles、permissions</li>
 * </ul>
 */
@Slf4j
public final class JwtUtil {

    private JwtUtil() {}

    /** accessToken 过期时间：30 分钟 */
    public static final long ACCESS_EXPIRATION = 30 * 60 * 1000L;

    /** refreshToken 过期时间：7 天 */
    public static final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 生成 accessToken（携带用户信息和权限）。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色列表（逗号分隔，如 "USER,VIP"）
     * @param permissions 权限列表（逗号分隔，如 "order:create,order:cancel"）
     * @return JWT token
     */
    public static String generateAccessToken(Long userId, String username,
                                              String roles, String permissions) {
        PrivateKey key = RSAKeyProvider.getPrivateKey();
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", "access")
                .claim("roles", roles != null ? roles : "")
                .claim("permissions", permissions != null ? permissions : "")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_EXPIRATION))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 简化版：生成 accessToken（无额外权限声明）。
     */
    public static String generateAccessToken(Long userId, String username) {
        return generateAccessToken(userId, username, "USER", "");
    }

    /**
     * 生成 refreshToken。
     */
    public static String generateRefreshToken(Long userId) {
        PrivateKey key = RSAKeyProvider.getPrivateKey();
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_EXPIRATION))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 解析 token，返回 Claims。
     * <p>使用 RSA 公钥验证签名。</p>
     */
    public static Claims parseToken(String token) {
        try {
            PublicKey key = RSAKeyProvider.getPublicKey();
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 token 是否有效（签名 + 过期时间）。
     */
    public static boolean validateToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && !claims.getExpiration().before(new Date());
    }

    /**
     * 从 token 中提取用户ID。
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 token 中提取用户名。
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }

    /**
     * 判断 token 是否为 refreshToken。
     */
    public static boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        return "refresh".equals(claims.get("type"));
    }

    /**
     * 从 token 中提取角色列表。
     */
    public static String getRoles(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return "";
        return claims.get("roles", String.class);
    }

    /**
     * 从 token 中提取权限列表。
     */
    public static String getPermissions(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return "";
        return claims.get("permissions", String.class);
    }
}
