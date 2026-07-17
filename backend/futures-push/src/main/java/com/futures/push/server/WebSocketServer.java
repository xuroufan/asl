package com.futures.push.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futures.push.service.PushService;
import com.futures.push.service.SubscriptionManager;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 服务端端点。
 *
 * <p>地址：{@code ws://host:8089/ws/{userId}}
 *
 * <p>支持的消息协议：
 * <ul>
 *   <li>{@code { "action": "subscribe", "type": "market", "symbols": ["HSI2309"] }}</li>
 *   <li>{@code { "action": "subscribe", "type": "order" }}</li>
 *   <li>{@code { "action": "unsubscribe", "type": "market", "symbols": ["HSI2309"] }}</li>
 *   <li>{@code { "action": "pong" }}</li>
 *   <li>{@code { "action": "ack", "seq": 12345 }}</li>
 * </ul>
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    private static PushService pushService;
    private static SubscriptionManager subscriptionManager;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Spring 无法直接注入静态成员，通过此方法注入。
     */
    @Autowired
    public void init(PushService ps, SubscriptionManager sm) {
        WebSocketServer.pushService = ps;
        WebSocketServer.subscriptionManager = sm;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        // 设置最大空闲超时（秒）
        session.setMaxIdleTimeout(0);
        // 设置消息最大长度（256KB）
        session.setMaxTextMessageBufferSize(256 * 1024);

        pushService.addSession(userId, session);

        // 重连后补发离线消息
        pushService.replayOfflineMessages(userId);

        // 发送连接成功通知
        sendJson(session, Map.of(
                "type", "connected",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
        ));

        log.info("WebSocket 连接打开: userId={}, sessionId={}", userId, session.getId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        pushService.removeSession(userId);
        log.info("WebSocket 连接关闭: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") String userId) {
        log.error("WebSocket 连接异常: userId={}", userId, error);
        pushService.removeSession(userId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String userId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);
            String action = (String) msg.getOrDefault("action", "");

            switch (action) {
                case "subscribe" -> handleSubscribe(userId, session, msg);
                case "unsubscribe" -> handleUnsubscribe(userId, msg);
                case "pong" -> pushService.handlePong(userId);
                case "ack" -> handleAck(userId, msg);
                default -> sendJson(session, Map.of(
                        "type", "error",
                        "msg", "未知 action: " + action
                ));
            }
        } catch (JsonProcessingException e) {
            log.warn("消息解析失败 userId={}: {}", userId, message);
            sendJson(session, Map.of("type", "error", "msg", "消息格式错误"));
        }
    }

    // ==================== 消息处理 ====================

    @SuppressWarnings("unchecked")
    private void handleSubscribe(String userId, Session session, Map<String, Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) {
            sendJson(session, Map.of("type", "error", "msg", "缺少订阅类型"));
            return;
        }

        switch (type) {
            case "market" -> {
                List<String> symbols = (List<String>) msg.get("symbols");
                if (symbols != null && !symbols.isEmpty()) {
                    subscriptionManager.subscribeMarkets(userId, Set.copyOf(symbols));
                    sendJson(session, Map.of(
                            "type", "subscribed",
                            "subscribeType", "market",
                            "symbols", symbols
                    ));
                    log.info("用户 {} 订阅行情: {}", userId, symbols);
                }
            }
            case "order" -> {
                subscriptionManager.subscribeOrder(userId);
                sendJson(session, Map.of("type", "subscribed", "subscribeType", "order"));
                log.info("用户 {} 订阅订单推送", userId);
            }
            default -> sendJson(session, Map.of("type", "error", "msg", "不支持的订阅类型: " + type));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUnsubscribe(String userId, Map<String, Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) return;

        switch (type) {
            case "market" -> {
                List<String> symbols = (List<String>) msg.get("symbols");
                if (symbols != null) {
                    symbols.forEach(s -> subscriptionManager.unsubscribeMarket(userId, s));
                }
            }
            case "order" -> subscriptionManager.unsubscribeOrder(userId);
            default -> log.warn("用户 {} 取消不支持的订阅类型: {}", userId, type);
        }
    }

    private void handleAck(String userId, Map<String, Object> msg) {
        Object seqObj = msg.get("seq");
        if (seqObj instanceof Number seq) {
            pushService.updateLastAckSeq(userId, seq.longValue());
            log.debug("用户 {} ACK seq={}", userId, seq.longValue());
        }
    }

    // ==================== 工具方法 ====================

    private void sendJson(Session session, Object data) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(data);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            log.warn("WebSocket 发送消息失败 sessionId={}", session.getId());
        }
    }
}
