package com.futures.fund.cache;

/**
 * 幂等性缓存接口。
 * <p>用于防止重复处理相同的业务请求（如重复扣款、重复冻结等）。</p>
 */
public interface IdempotentCache {

    /**
     * 检查键是否已存在。
     * @param key 幂等键
     * @return true 表示已处理过
     */
    boolean hasKey(String key);

    /**
     * 设置幂等键，超时后自动失效。
     * @param key      幂等键
     * @param value    值
     * @param ttlHours 超时时间（小时）
     */
    void set(String key, String value, long ttlHours);
}
