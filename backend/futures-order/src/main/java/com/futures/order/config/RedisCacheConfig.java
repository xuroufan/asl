package com.futures.order.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 缓存配置 — 随机 TTL 防缓存雪崩
 *
 * 为每个缓存设置 baseTTL ± 随机偏移, 防止大量 key 同时过期。
 * 基础 TTL 5min, 随机偏移 ±120s。
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private Duration ttlWithJitter(long baseSeconds) {
        long jitter = ThreadLocalRandom.current().nextLong(-120, 121);
        return Duration.ofSeconds(Math.max(30, baseSeconds + jitter));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("user:positions",   config(600)); // 10 min
        configMap.put("user:orders",      config(300)); //  5 min
        configMap.put("market:quotes",    config(60));  //  1 min
        configMap.put("fund:account",     config(300));
        configMap.put("risk:check",       config(120));
        configMap.put("risk:limit",       config(600));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config(300))
                .withInitialCacheConfigurations(configMap)
                .build();
    }

    private RedisCacheConfiguration config(long baseSeconds) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttlWithJitter(baseSeconds))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));
    }
}
