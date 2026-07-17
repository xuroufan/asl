package com.futures.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.List;

/**
 * RocketMQ 消息转换器配置
 * <p>
 * 确保 RocketMQ 的序列化/反序列化使用 Jackson + JavaTimeModule
 * 以正确支持 {@link java.time.LocalDateTime} 等 Java 8 时间类型。
 */
@Configuration
public class OrderRocketMQConfig {

    /**
     * 替换 RocketMQ 默认的消息转换器
     * <p>
     * 默认的 MappingJackson2MessageConverter 不支持 Java 8 时间类型序列化，
     * 此处注册 JavaTimeModule 以解决 LocalDateTime 序列化问题。
     */
    @Bean
    public RocketMQMessageConverter rocketMQMessageConverter() {
        return new RocketMQMessageConverter() {
            @Override
            public CompositeMessageConverter getMessageConverter() {
                // 获取 RocketMQ 默认的消息转换器列表
                List<MessageConverter> converters = List.of(
                        createJacksonConverter(),
                        // 保留原始 String/byte[] 转换
                        new org.springframework.messaging.converter.StringMessageConverter(),
                        new org.springframework.messaging.converter.ByteArrayMessageConverter()
                );
                return new CompositeMessageConverter(converters);
            }

            private MappingJackson2MessageConverter createJacksonConverter() {
                MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.configure(
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
                objectMapper.configure(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        false);
                converter.setObjectMapper(objectMapper);
                return converter;
            }
        };
    }
}
