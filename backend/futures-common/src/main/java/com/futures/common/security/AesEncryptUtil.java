package com.futures.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 GCM 敏感数据加密工具类。
 * <p>
 * 用于加密身份证号、手机号等数据库中需要保护的敏感字段。
 * 加密密钥从环境变量 {@code FUTURES_AES_KEY} 读取，需为 32 字节 Base64 编码。
 * 若未设置环境变量，使用默认密钥（生产环境必须替换）。
 * </p>
 *
 * 加密模式：AES-256-GCM（认证加密，自带完整性校验）
 * IV 长度：12 字节（随机生成，随密文一起存储）
 * Tag 长度：128 位
 *
 * 密文格式：Base64(IV(12字节) + 密文)
 */
@Slf4j
public final class AesEncryptUtil {

    private AesEncryptUtil() {}

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 32; // 256 bits

    /** AES 密钥（从环境变量读取，生产环境必须配置） */
    private static final byte[] SECRET_KEY;

    static {
        String envKey = System.getenv("FUTURES_AES_KEY");
        if (envKey != null && !envKey.isBlank()) {
            SECRET_KEY = Base64.getDecoder().decode(envKey);
            if (SECRET_KEY.length != AES_KEY_LENGTH) {
                log.warn("FUTURES_AES_KEY 长度 {} 不符合 32 字节要求，使用默认密钥", SECRET_KEY.length);
            }
        } else {
            // 默认密钥（仅用于开发测试，生产环境必须通过环境变量配置）
            log.warn("未设置 FUTURES_AES_KEY 环境变量，使用默认 AES 密钥（不安全）");
            SECRET_KEY = new byte[AES_KEY_LENGTH];
            new SecureRandom().nextBytes(SECRET_KEY);
        }
    }

    private static final SecretKeySpec KEY_SPEC = new SecretKeySpec(SECRET_KEY, AES_ALGORITHM);

    /**
     * 加密明文。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（含 IV）
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, KEY_SPEC, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ciphertext.length);
            buf.put(iv);
            buf.put(ciphertext);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密密文。
     *
     * @param ciphertextBase64 Base64 编码的密文（含 IV）
     * @return 明文
     */
    public static String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isEmpty()) return ciphertextBase64;
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertextBase64);
            ByteBuffer buf = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);

            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, KEY_SPEC, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 判断字符串是否已加密（Base64 编码且含 IV）。
     */
    public static boolean isEncrypted(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(str);
            return decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
