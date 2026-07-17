package com.futures.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.security.PrivateKey;
import java.security.PublicKey;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test @DisplayName("RSAKeyProvider 能加载或生成密钥对")
    void testKeyProviderLoads() {
        PrivateKey priv = RSAKeyProvider.getPrivateKey();
        PublicKey pub = RSAKeyProvider.getPublicKey();
        assertNotNull(priv, "私钥不应为空");
        assertNotNull(pub, "公钥不应为空");
    }

    @Test @DisplayName("RSAKeyProvider 公私钥算法均为RSA")
    void testKeyAlgorithm() {
        assertEquals("RSA", RSAKeyProvider.getPrivateKey().getAlgorithm());
        assertEquals("RSA", RSAKeyProvider.getPublicKey().getAlgorithm());
    }
}
