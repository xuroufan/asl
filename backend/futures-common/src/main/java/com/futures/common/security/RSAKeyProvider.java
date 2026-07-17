package com.futures.common.security;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对提供者（RS256 JWT 签名）。
 * <p>
 * 自动在 ~/.futures/keys/ 生成或加载 2048 位 RSA 密钥对。
 * 生产环境应使用 KMS / Vault 管理密钥。
 * </p>
 */
@Slf4j
public final class RSAKeyProvider {

    private RSAKeyProvider() {}

    private static final String KEY_DIR;
    private static final String PRIVATE_KEY_FILE;
    private static final String PUBLIC_KEY_FILE;
    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;

    static {
        String envDir = System.getenv("FUTURES_KEY_DIR");
        KEY_DIR = (envDir != null && !envDir.isBlank()) ? envDir
                : System.getProperty("user.home") + "/.futures/keys";
        PRIVATE_KEY_FILE = KEY_DIR + "/jwt-private.pem";
        PUBLIC_KEY_FILE = KEY_DIR + "/jwt-public.pem";
        initialize();
    }

    private static void initialize() {
        try {
            Files.createDirectories(Paths.get(KEY_DIR));
            File privFile = new File(PRIVATE_KEY_FILE);
            File pubFile = new File(PUBLIC_KEY_FILE);
            if (privFile.exists() && pubFile.exists()) {
                privateKey = loadPrivateKey(privFile);
                publicKey = loadPublicKey(pubFile);
                log.info("RSA 密钥对加载成功（{}）", KEY_DIR);
                return;
            }
            log.info("生成 2048 位 RSA 密钥对…");
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
            saveKey(privFile, privateKey.getEncoded(), "PRIVATE KEY");
            saveKey(pubFile, publicKey.getEncoded(), "PUBLIC KEY");
            log.info("RSA 密钥对已保存至 {}", KEY_DIR);
        } catch (Exception e) {
            log.error("RSA 初始化失败，使用临时密钥", e);
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                KeyPair pair = gen.generateKeyPair();
                privateKey = pair.getPrivate();
                publicKey = pair.getPublic();
            } catch (Exception ex) {
                throw new RuntimeException("RSA 密钥对生成失败", ex);
            }
        }
    }

    public static PrivateKey getPrivateKey() { return privateKey; }
    public static PublicKey getPublicKey() { return publicKey; }

    private static PrivateKey loadPrivateKey(File file) throws Exception {
        String pem = Files.readString(file.toPath(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    private static PublicKey loadPublicKey(File file) throws Exception {
        String pem = Files.readString(file.toPath(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    private static void saveKey(File file, byte[] encoded, String label) throws IOException {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        Files.writeString(file.toPath(), "-----BEGIN " + label + "-----\n" + b64 + "\n-----END " + label + "-----\n");
    }
}
