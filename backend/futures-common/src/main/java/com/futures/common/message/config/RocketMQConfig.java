package com.futures.common.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * RocketMQ 消息转换器配置。
 * <p>
 * 注册 Jackson 消息转换器，支持 Java 8+ 时间类型（LocalDateTime、LocalDate 等）
 * 的序列化与反序列化，确保事件消息在 Producer/Consumer 之间正确传递。
 */
@Configuration
public class RocketMQConfig {

    /**
     * 配置 RocketMQ Jackson 消息转换器。
     * 所有事件对象通过 JSON 序列化传输。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setSerializedPayloadClass(String.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(mapper);

        return converter;
    }
}
