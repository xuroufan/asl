package com.futures.push.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置。
 *
 * <p>注册 {@link ServerEndpointExporter} Bean，使 Spring Boot 能够自动发现
 * 并注册所有标注了 {@code @ServerEndpoint} 的 WebSocket 端点。
 *
 * <p>端点地址：{@code ws://host:8089/ws/{userId}}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
