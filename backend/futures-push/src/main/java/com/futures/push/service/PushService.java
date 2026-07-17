package com.futures.push.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.futures.push.store.OfflineMessageStore;

/**
 * WebSocket 推送服务。
 *
 * <p>集中管理所有 WebSocket Session（用户ID → Session），
 * 提供消息推送、离线消息补发和心跳检测功能。
 */
@Slf4j
@Service
public class PushService {

    /** 用户ID → WebSocket Session */
    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /** 用户ID → 最后确认的推送序列号 */
    private final ConcurrentHashMap<String, Long> lastAckMap = new ConcurrentHashMap<>();

    /** 用户ID → 连续心跳失败次数 */
    private final ConcurrentHashMap<String, Integer> heartbeatFailCount = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private OfflineMessageStore offlineMessageStore;

    @Autowired
    private SubscriptionManager subscriptionManager;

    /** 最大心跳失败次数 */
    static final int MAX_HEARTBEAT_FAILURES = 3;

    public PushService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ==================== 连接管理 ====================

    public void addSession(String userId, Session session) {
        sessionMap.put(userId, session);
        heartbeatFailCount.put(userId, 0);
        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    public void removeSession(String userId) {
        Session session = sessionMap.remove(userId);
        heartbeatFailCount.remove(userId);
        subscriptionManager.removeUserSubscriptions(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("关闭 Session 异常 userId={}", userId, e);
            }
        }
        log.info("WebSocket 连接断开: userId={}", userId);
    }

    public Session getSession(String userId) {
        return sessionMap.get(userId);
    }

    public boolean isConnected(String userId) {
        Session session = sessionMap.get(userId);
        return session != null && session.isOpen();
    }

    public int getActiveConnectionCount() {
        return sessionMap.size();
    }

    // ==================== 消息推送 ====================

    /**
     * 向指定用户推送消息。如果用户不在线，转存到离线消息存储。
     */
    public void pushToUser(String userId, Object message) {
        Session session = getSession(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        } else {
            storeOffline(userId, message);
        }
    }

    /**
     * 向订阅了指定合约的所有在线用户推送行情。
     */
    public void pushToMarketSubscribers(String symbol, Object message) {
        var subscribers = subscriptionManager.getSymbolSubscribers(symbol);
        for (String userId : subscribers) {
            pushToUser(userId, message);
        }
    }

    /**
     * 向指定用户补发离线期间的消息。
     */
    public void replayOfflineMessages(String userId) {
        if (offlineMessageStore == null) {
            return;
        }
        long lastSeq = lastAckMap.getOrDefault(userId, 0L);
        List<String> pendingMessages = offlineMessageStore.getPendingMessages(userId, lastSeq);
        if (pendingMessages.isEmpty()) {
            log.debug("用户 {} 无离线消息需要补发", userId);
            return;
        }

        Session session = getSession(userId);
        if (session == null || !session.isOpen()) {
            log.warn("用户 {} 已离线，跳过离线消息补发", userId);
            return;
        }

        int count = 0;
        try {
            // 使用批量推送格式
            String batchJson = objectMapper.writeValueAsString(Map.of(
                    "type", "offline_batch",
                    "count", pendingMessages.size(),
                    "messages", pendingMessages.stream()
                            .map(this::parseJson)
                            .toList()
            ));
            sendRaw(session, batchJson);
            count = pendingMessages.size();
        } catch (JsonProcessingException e) {
            log.error("序列化离线消息批失败 userId={}", userId, e);
        }

        offlineMessageStore.clearMessages(userId);
        lastAckMap.put(userId, offlineMessageStore.getLastSeq(userId));
        log.info("用户 {} 离线消息补发完成: {} 条", userId, count);
    }

    // ==================== 心跳检测 ====================

    /**
     * 发送心跳 ping 消息给所有连接。
     */
    public void sendHeartbeatPing() {
        String pingJson;
        try {
            pingJson = objectMapper.writeValueAsString(Map.of("type", "ping", "timestamp", System.currentTimeMillis()));
        } catch (JsonProcessingException e) {
            log.error("序列化 ping 消息失败", e);
            return;
        }

        sessionMap.forEach((userId, session) -> {
            if (session.isOpen()) {
                sendRaw(session, pingJson);
            }
        });
    }

    /**
     * 处理客户端回复的 pong 消息。
     */
    public void handlePong(String userId) {
        heartbeatFailCount.put(userId, 0);
    }

    /**
     * 检查心跳失败次数，断开超时连接。
     */
    public void checkHeartbeatTimeouts() {
        heartbeatFailCount.forEach((userId, failCount) -> {
            if (failCount >= MAX_HEARTBEAT_FAILURES) {
                log.warn("心跳超时，断开连接 userId={}", userId);
                removeSession(userId);
            }
        });
    }

    /**
     * 增加某用户的心跳失败计数。
     */
    public void incrementHeartbeatFail(String userId) {
        heartbeatFailCount.merge(userId, 1, Integer::sum);
    }

    // ==================== 序列号更新 ====================

    public void updateLastAckSeq(String userId, long seq) {
        lastAckMap.put(userId, seq);
    }

    // ==================== 内部方法 ====================

    private void sendMessage(Session session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sendRaw(session, json);
        } catch (JsonProcessingException e) {
            log.error("序列化推送消息失败", e);
        }
    }

    private void sendRaw(Session session, String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (IOException e) {
            log.warn("WebSocket 发送消息失败 sessionId={}", session.getId());
        }
    }

    private void storeOffline(String userId, Object message) {
        if (offlineMessageStore != null) {
            offlineMessageStore.storeMessage(userId, message);
        }
    }

    private Map<?, ?> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("解析离线消息 JSON 失败", e);
            return Map.of("raw", json);
        }
    }
}
