package com.futures.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置加密工具类
 *
 * 用途：加密 Nacos 配置中的敏感信息
 * - 数据库密码 (spring.datasource.password)
 * - Redis 密码 (spring.data.redis.password)
 * - API 密钥 (futures.market.data-source.api-key)
 *
 * 加密方式：AES-256-GCM（认证加密，自带完整性校验）
 * 解密密钥：通过环境变量 CONFIG_DECRYPT_KEY 注入
 *
 * 使用示例：
 *   // 加密
 *   String encrypted = ConfigCipher.encrypt("futures123");
 *   // 输出: ENC(A7x3...)
 *
 *   // 在Nacos配置中使用
 *   spring.datasource.password: ENC(A7x3...)
 *
 *   // 在应用启动时解密
 *   application.yml 中配置:
 *   spring.datasource.password: ${CONFIG_DECRYPT_PASSWORD:futures123}
 *
 * 安全建议：
 * 1. 生产环境通过 K8s Secret 注入 CONFIG_DECRYPT_KEY
 * 2. 密钥定期轮换（建议90天）
 * 3. 环境变量不写入任何配置文件或版本控制
 */
@Slf4j
public class ConfigCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // GCM 推荐 12 字节 IV
    private static final int GCM_TAG_LENGTH = 128;   // 128 位认证标签
    private static final int AES_KEY_SIZE = 256;     // AES-256
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    /**
     * 获取解密密钥（从环境变量读取）
     * 开发环境默认值用于本地测试，生产环境必须通过 K8s Secret 注入
     */
    private static SecretKey getSecretKey() {
        String keyStr = System.getenv("CONFIG_DECRYPT_KEY");
        if (keyStr == null || keyStr.isEmpty()) {
            // 开发环境默认密钥（仅用于本地测试）
            keyStr = "FuturesPlatformDevKey2024!!";
            log.warn("CONFIG_DECRYPT_KEY 环境变量未设置，使用开发环境默认密钥");
        }

        // 确保密钥长度为 32 字节 (AES-256)
        byte[] keyBytes = new byte[32];
        byte[] inputBytes = keyStr.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(inputBytes, 0, keyBytes, 0, Math.min(inputBytes.length, 32));

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密敏感配置
     *
     * @param plainText 明文密码或密钥
     * @return 加密后的字符串，格式: ENC(Base64(IV + ciphertext))
     */
    public static String encrypt(String plainText) {
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合 IV + 密文
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(buffer.array()) + ENC_SUFFIX;
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("配置加密失败: " + e.getMessage());
        }
    }

    /**
     * 解密配置
     *
     * @param encryptedText 加密后的配置值（ENC(...) 格式）
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        // 检查是否为加密格式
        if (!encryptedText.startsWith(ENC_PREFIX) || !encryptedText.endsWith(ENC_SUFFIX)) {
            return encryptedText; // 非加密格式，直接返回
        }

        try {
            // 提取 Base64 编码的密文
            String base64 = encryptedText.substring(
                ENC_PREFIX.length(),
                encryptedText.length() - ENC_SUFFIX.length()
            );
            byte[] decoded = Base64.getDecoder().decode(base64);

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // 提取 IV（前 12 字节）
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // 提取密文（剩余字节）
            byte[] ciphertext = new byte[decoded.length - GCM_IV_LENGTH];
            buffer.get(ciphertext);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败 (可能密钥不正确)", e);
            return "***DECRYPT_FAILED***";
        }
    }

    /**
     * 提供主方法用于命令行加密
     * java com.futures.common.util.ConfigCipher "my_password"
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: java ConfigCipher <明文密码>");
            System.out.println("示例: java ConfigCipher 'futures123'");
            return;
        }

        String plainText = args[0];
        String encrypted = encrypt(plainText);
        System.out.println("原始值: " + plainText);
        System.out.println("加密值: " + encrypted);
        System.out.println("\n请将以上加密值放入 Nacos 配置中");
        System.out.println("并确保环境变量 CONFIG_DECRYPT_KEY 已设置");

        // 验证解密
        String decrypted = decrypt(encrypted);
        System.out.println("验证解密: " + (plainText.equals(decrypted) ? "✓ 成功" : "✗ 失败"));
    }
}
