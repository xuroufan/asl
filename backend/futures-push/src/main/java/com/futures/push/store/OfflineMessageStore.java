package com.futures.push.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 离线消息存储。
 *
 * <p>用户断线期间产生的推送消息暂存到 Redis，重连后由 {@code PushService}
 * 补发给用户。每条消息附带一个单调递增的序列号，方便补发时定位断点。
 *
 * <p>Redis Key 结构：
 * <ul>
 *   <li>{@code push:offline:{userId}:seq} — 最后推送的序列号</li>
 *   <li>{@code push:offline:{userId}:msg:{seq}} — 消息体</li>
 * </ul>
 *
 * <p>每条消息 TTL=30 分钟，用户重连后消费完毕后清理。
 */
@Slf4j
@Component
public class OfflineMessageStore {

    private static final String KEY_PREFIX = "push:offline:";
    private static final String SEQ_SUFFIX = ":seq";
    private static final String MSG_SUFFIX = ":msg:";
    private static final long MESSAGE_TTL_SECONDS = 1800; // 30分钟

    private final AtomicLong globalSeq = new AtomicLong(0);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public OfflineMessageStore() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        if (redisTemplate == null) {
            log.warn("Redis 不可用，离线消息功能将被禁用");
        } else {
            log.info("OfflineMessageStore initialized with Redis");
        }
    }

    /**
     * 存储一条离线消息。
     */
    public void storeMessage(String userId, Object message) {
        if (redisTemplate == null) {
            return;
        }
        try {
            long seq = globalSeq.incrementAndGet();
            String msgKey = KEY_PREFIX + userId + MSG_SUFFIX + seq;
            String seqKey = KEY_PREFIX + userId + SEQ_SUFFIX;

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForValue().set(msgKey, json, MESSAGE_TTL_SECONDS, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(seqKey, String.valueOf(seq), MESSAGE_TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("存储离线消息 userId={} seq={}", userId, seq);
        } catch (Exception e) {
            log.error("存储离线消息失败 userId={}", userId, e);
        }
    }

    /**
     * 获取用户最后已知的推送序列号。
     */
    public long getLastSeq(String userId) {
        if (redisTemplate == null) {
            return 0;
        }
        try {
            String seqStr = redisTemplate.opsForValue().get(KEY_PREFIX + userId + SEQ_SUFFIX);
            return seqStr != null ? Long.parseLong(seqStr) : 0;
        } catch (Exception e) {
            log.warn("获取离线序列号失败 userId={}", userId);
            return 0;
        }
    }

    /**
     * 获取用户断线期间的所有消息（根据最后确认的序列号）。
     */
    public List<String> getPendingMessages(String userId, long lastAckSeq) {
        List<String> messages = new ArrayList<>();
        if (redisTemplate == null) {
            return messages;
        }
        try {
            long currentSeq = getLastSeq(userId);
            for (long seq = lastAckSeq + 1; seq <= currentSeq; seq++) {
                String msgKey = KEY_PREFIX + userId + MSG_SUFFIX + seq;
                String json = redisTemplate.opsForValue().get(msgKey);
                if (json != null) {
                    messages.add(json);
                }
            }
        } catch (Exception e) {
            log.warn("获取离线消息失败 userId={}", userId, e);
        }
        return messages;
    }

    /**
     * 清理用户的离线消息（补发完毕后调用）。
     */
    public void clearMessages(String userId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            long lastSeq = getLastSeq(userId);
            for (long seq = 1; seq <= lastSeq; seq++) {
                String msgKey = KEY_PREFIX + userId + MSG_SUFFIX + seq;
                redisTemplate.delete(msgKey);
            }
            redisTemplate.delete(KEY_PREFIX + userId + SEQ_SUFFIX);
            log.debug("清理离线消息 userId={} seq=1~{}", userId, lastSeq);
        } catch (Exception e) {
            log.warn("清理离线消息失败 userId={}", userId, e);
        }
    }
}
