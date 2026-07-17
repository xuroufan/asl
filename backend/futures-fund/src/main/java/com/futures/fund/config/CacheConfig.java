package com.futures.fund.config;

import com.futures.common.util.RedisUtil;
import com.futures.fund.cache.IdempotentCache;
import com.futures.fund.cache.InMemoryIdempotentCache;
import com.futures.fund.cache.RedisIdempotentCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 缓存组件配置。
 * <p>
 * 当 Redis 可用时使用 {@link RedisIdempotentCache}，
 * 否则使用 {@link InMemoryIdempotentCache} 降级。
 * </p>
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnBean(RedisUtil.class)
    @Primary
    public IdempotentCache redisIdempotentCache(RedisUtil redisUtil) {
        return new RedisIdempotentCache(redisUtil);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentCache.class)
    public IdempotentCache inMemoryIdempotentCache() {
        return new InMemoryIdempotentCache();
    }
}
