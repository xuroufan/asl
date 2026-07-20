package com.futures.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // 开发环境允许的来源 — 生产环境替换为实际域名
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173", "http://localhost:8090", "http://localhost:3000",
            "http://127.0.0.1:5173", "http://127.0.0.1:8090", "http://127.0.0.1:3000"
    );

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境: config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedOriginPatterns(List.of("*")); // 开发环境宽松

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    public org.springframework.cloud.gateway.filter.GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            var headers = exchange.getResponse().getHeaders();
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");
            headers.add("X-Content-Security-Policy", "frame-ancestors 'self'");
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            return chain.filter(exchange);
        };
    }

    }
}
