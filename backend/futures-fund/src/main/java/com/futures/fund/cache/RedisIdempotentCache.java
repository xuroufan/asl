package com.futures.fund.cache;

import com.futures.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的幂等性缓存实现。
 * <p>包装 {@link RedisUtil}，提供幂等键的存取和自动过期。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotentCache implements IdempotentCache {

    private final RedisUtil redisUtil;

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisUtil.hasKey(key));
    }

    @Override
    public void set(String key, String value, long ttlHours) {
        redisUtil.set(key, value, ttlHours, TimeUnit.HOURS);
    }
}
