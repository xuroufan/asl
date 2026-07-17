package com.futures.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置 — 用于调用 futures-risk 等微服务 REST API。
 * <p>注意：生产环境建议改用 Spring Cloud OpenFeign 或 WebClient。</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 连接超时 5 秒
        factory.setReadTimeout(10000);     // 读取超时 10 秒
        return new RestTemplate(factory);
    }
}
