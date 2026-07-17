package com.futures.matching.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义 Prometheus 指标端点。
 * <p>
 * 撮合引擎的 actuator 端点存在 Tomcat 兼容性问题（Java 26），
 * 因此绕过 /actuator/prometheus，改为通过 Spring MVC 控制器直接暴露指标。
 */
@Configuration
public class PrometheusMetricsConfig {

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @RestController
    static class PrometheusController {
        private final PrometheusMeterRegistry registry;

        public PrometheusController(PrometheusMeterRegistry registry) {
            this.registry = registry;
        }

        @GetMapping("/metrics/prometheus")
        public String scrape() {
            return registry.scrape() + "# EOF\n";
        }
    }
}
