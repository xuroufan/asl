package com.futures.fund.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的幂等性缓存实现（降级/测试用）。
 * <p>当 Redis 不可用时使用，仅适用于单实例场景。</p>
 */
@Slf4j
public class InMemoryIdempotentCache implements IdempotentCache {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public boolean hasKey(String key) {
        return cache.containsKey(key);
    }

    @Override
    public void set(String key, String value, long ttlHours) {
        cache.put(key, value);
        log.debug("InMemoryIdempotentCache: set key={}, value={}, ttl={}h", key, value, ttlHours);
    }
}
