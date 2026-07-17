package com.futures.account.util;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP (Time-based One-Time Password) 工具类。
 * <p>实现 RFC 6238 兼容的 TOTP 算法，可配合 Google Authenticator 等应用使用。</p>
 */
public class TOTPUtil {

    private TOTPUtil() {}

    /** 默认时间步长：30 秒 */
    private static final int TIME_STEP = 30;

    /** TOTP 验证容许的偏移窗口（前后各 1 步） */
    private static final int WINDOW = 1;

    /** 生成 TOTP 密钥长度（字节数） */
    private static final int SECRET_BYTES = 20;

    /**
     * 生成一个随机的 TOTP 密钥（Base32 编码）。
     *
     * @return Base32 编码的密钥字符串
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    /**
     * 根据当前时间生成 6 位 TOTP 验证码。
     *
     * @param secret Base32 编码的密钥
     * @return 6 位数字验证码
     */
    public static String generateCode(String secret) {
        return generateCode(secret, System.currentTimeMillis() / 1000 / TIME_STEP);
    }

    /**
     * 根据指定时间步长生成 TOTP 验证码。
     *
     * @param secret   Base32 编码的密钥
     * @param timeStep 时间步长（Unix 时间 / TIME_STEP）
     * @return 6 位数字验证码
     */
    public static String generateCode(String secret, long timeStep) {
        try {
            byte[] key = Base32.decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int code = binary % 1000000;
            return String.format("%06d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP 验证码生成失败", e);
        }
    }

    /**
     * 验证用户输入的 TOTP 验证码（容许前后各 1 个时间步长的偏移）。
     *
     * @param secret   Base32 编码的密钥
     * @param userCode 用户输入的 6 位验证码
     * @return 验证通过返回 true
     */
    public static boolean verifyCode(String secret, String userCode) {
        long currentStep = System.currentTimeMillis() / 1000 / TIME_STEP;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            String expected = generateCode(secret, currentStep + i);
            if (expected.equals(userCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成 Google Authenticator 兼容的 otpauth URL（用于生成二维码）。
     *
     * @param issuer  发行方（如 "Futures"）
     * @param account 用户账户名
     * @param secret  Base32 编码的密钥
     * @return otpauth URL
     */
    public static String generateQRUrl(String issuer, String account, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=%d",
                issuer, account, secret, issuer, TIME_STEP
        );
    }

    /**
     * Base32 编解码（RFC 4648 标准）。
     */
    private static class Base32 {
        private static final char[] ALPHABET = {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', '2', '3', '4', '5', '6', '7'
        };
        private static final int[] BITS = new int[128];

        static {
            for (int i = 0; i < BITS.length; i++) BITS[i] = -1;
            for (int i = 0; i < ALPHABET.length; i++) BITS[ALPHABET[i]] = i;
        }

        static String encode(byte[] data) {
            StringBuilder result = new StringBuilder();
            int buffer = 0, bitsLeft = 0;
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xff);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    result.append(ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1f]);
                    bitsLeft -= 5;
                }
            }
            if (bitsLeft > 0) {
                result.append(ALPHABET[(buffer << (5 - bitsLeft)) & 0x1f]);
            }
            return result.toString();
        }

        static byte[] decode(String str) {
            byte[] data = str.toUpperCase().replaceAll("[^A-Z2-7]", "").getBytes();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int buffer = 0, bitsLeft = 0;
            for (byte b : data) {
                if (BITS[b] < 0) continue;
                buffer = (buffer << 5) | BITS[b];
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    baos.write((buffer >> (bitsLeft - 8)) & 0xff);
                    bitsLeft -= 8;
                }
            }
            return baos.toByteArray();
        }
    }
}
