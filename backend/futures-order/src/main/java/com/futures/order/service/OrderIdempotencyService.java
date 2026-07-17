package com.futures.order.service;

import com.futures.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 订单幂等服务
 * <p>
 * 使用 Redis 缓存已处理的 clientOrderId，有效期 24 小时。
 * 如果客户端的 orderId 被重复提交，返回已存在的订单标识。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdempotencyService {

    private static final String IDEMPOTENT_KEY_PREFIX = "order:idempotent:";
    private static final long TTL_HOURS = 24;

    private final RedisUtil redisUtil;

    /**
     * 尝试标记 clientOrderId 为"已处理"
     *
     * @param clientOrderId 客户端订单号
     * @return true 表示第一次请求（幂等键不存在），false 表示重复请求
     */
    public boolean tryMarkProcessed(String clientOrderId) {
        if (clientOrderId == null || clientOrderId.isBlank()) {
            return true; // 没有 clientOrderId 则不检查幂等
        }
        String key = IDEMPOTENT_KEY_PREFIX + clientOrderId;
        Boolean absent = redisUtil.setIfAbsent(key, "1");
        if (absent != null && absent) {
            redisUtil.expire(key, TTL_HOURS, TimeUnit.HOURS);
            log.debug("幂等标记已创建: clientOrderId={}", clientOrderId);
            return true;
        }
        log.warn("检测到重复 clientOrderId: {}", clientOrderId);
        return false;
    }

    /**
     * 存储订单号到幂等键
     */
    public void bindOrderId(String clientOrderId, Long orderId) {
        if (clientOrderId == null || clientOrderId.isBlank()) return;
        String key = IDEMPOTENT_KEY_PREFIX + clientOrderId;
        redisUtil.set(key, String.valueOf(orderId), TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 查询已存在的订单号
     */
    public Long getExistingOrderId(String clientOrderId) {
        if (clientOrderId == null || clientOrderId.isBlank()) return null;
        String key = IDEMPOTENT_KEY_PREFIX + clientOrderId;
        String val = redisUtil.get(key);
        return val != null ? Long.parseLong(val) : null;
    }
}
