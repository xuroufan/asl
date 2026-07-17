package com.futures.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 网关 CORS 配置 — 生产环境必须限定允许的来源域名
 *
 * 安全基线:
 * - 不允许通配符 "*" (不安全)
 * - 明确列出允许的前端域名
 * - 不暴露敏感响应头
 */
@Configuration
public class CorsConfig {

    // ============================================================
    // 生产环境请修改为实际域名:
    //   "https://futures-platform.com",
    //   "https://admin.futures-platform.com",
    //   "https://tws.futures-platform.com"
    // ============================================================
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",   // 期货终端 (开发)
            "http://localhost:5174",   // 管理后台 (开发)
            "http://localhost:3001",   // TWS Lite (开发)
            "http://localhost:4181",   // 生产构建预览
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://127.0.0.1:3001"
            // ⚠ 生产环境请替换为实际域名
    );

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));  // 开发环境宽松
        // 生产环境用: config.setAllowedOrigins(ALLOWED_ORIGINS);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
