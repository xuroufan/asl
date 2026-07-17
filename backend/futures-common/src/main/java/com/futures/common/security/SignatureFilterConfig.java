package com.futures.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 签名校验过滤器注册配置。
 * <p>
 * 通过 {@code futures.security.signature.enabled=true} 启用。
 * 默认关闭，仅在需要额外 API 签名验证的场景开启（如对接第三方或高安全要求环境）。
 * </p>
 */
@Slf4j
@Configuration
public class SignatureFilterConfig {

    @Value("${futures.security.signature.enabled:false}")
    private boolean signatureEnabled;

    @Value("${futures.security.signature.app-secret:futures-default-secret-2024}")
    private String appSecret;

    @Bean
    @ConditionalOnProperty(name = "futures.security.signature.enabled", havingValue = "true")
    public FilterRegistrationBean<SignatureFilter> signatureFilterRegistration(SignatureFilter filter) {
        filter.setEnabled(true);
        filter.setAppSecret(appSecret);
        FilterRegistrationBean<SignatureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(-50);
        log.info("API 签名校验已启用，secret={}", appSecret.substring(0, Math.min(4, appSecret.length())) + "****");
        return registration;
    }
}
