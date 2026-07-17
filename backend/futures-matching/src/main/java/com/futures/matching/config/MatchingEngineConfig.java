package com.futures.matching.config;

import com.futures.matching.engine.MatchingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 撮合引擎 Spring 配置
 */
@Slf4j
@Configuration
public class MatchingEngineConfig {

    /**
     * 默认撮合引擎实例（用于单品种快速启动）。
     * 多品种场景由 {@link com.futures.matching.engine.OrderBookManager} 管理。
     */
    @Bean
    public MatchingEngine matchingEngine() {
        return new MatchingEngine("HSI");
    }
}
