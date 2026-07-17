package com.futures.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密配置。
 * <p>提供 BCryptPasswordEncoder Bean，用于密码加密和验证。</p>
 */
@Configuration
public class BCryptConfig {

    /**
     * BCryptPasswordEncoder Bean，加密强度为 10。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
