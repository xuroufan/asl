package com.futures.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * API 请求签名工具类（HMAC-SHA256）。
 * <p>
 * 用于防重放攻击：客户端使用 AppSecret 对请求参数签名，
 * 服务端使用相同的 Secret 验证签名有效性。
 * </p>
 *
 * 签名算法：
 * <pre>
 * sign = HMAC-SHA256(METHOD + URI + Timestamp + Body, AppSecret)
 * </pre>
 *
 * 服务端校验流程：
 * <ol>
 *   <li>检查时间戳是否在 5 分钟内（防止重放）</li>
 *   <li>检查 nonce 是否已被使用（Redis 缓存 5 分钟）</li>
 *   <li>计算签名并与请求签名比对</li>
 * </ol>
 */
@Slf4j
public final class SignatureUtil {

    private SignatureUtil() {}

    /** 签名字符集 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 时间戳有效窗口：5 分钟 */
    public static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;

    /**
     * 生成 HMAC-SHA256 签名。
     *
     * @param secret    API Secret 密钥
     * @param method    HTTP 方法（GET/POST/PUT/DELETE）
     * @param uri       请求路径（不含查询参数）
     * @param timestamp 毫秒时间戳
     * @param nonce     随机字符串（防重放）
     * @param body      请求体字符串（GET 请求传空字符串）
     * @return Base64 编码的签名字符串
     */
    public static String sign(String secret, String method, String uri,
                               long timestamp, String nonce, String body) {
        String payload = method.toUpperCase() + "\n"
                + uri + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + (body != null ? body : "");
        return hmacSha256(secret, payload);
    }

    /**
     * 验证请求签名。
     *
     * @param secret      API Secret
     * @param method      HTTP 方法
     * @param uri         请求路径
     * @param timestamp   请求中的时间戳
     * @param nonce       请求中的随机数
     * @param body        请求体
     * @param signHeader  请求中的签名值
     * @return true=签名有效
     */
    public static boolean verify(String secret, String method, String uri,
                                  long timestamp, String nonce, String body, String signHeader) {
        // 1. 检查时间戳是否在有效窗口内
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > TIMESTAMP_WINDOW_MS) {
            log.warn("签名时间戳超时: now={}, ts={}, diff={}ms", now, timestamp, Math.abs(now - timestamp));
            return false;
        }

        // 2. 计算并比对签名
        String expected = sign(secret, method, uri, timestamp, nonce, body);
        boolean valid = expected.equals(signHeader);
        if (!valid) {
            log.warn("签名不匹配: expected={}, received={}", expected, signHeader);
        }
        return valid;
    }

    /**
     * HMAC-SHA256 计算。
     */
    private static String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 签名失败", e);
        }
    }
}
