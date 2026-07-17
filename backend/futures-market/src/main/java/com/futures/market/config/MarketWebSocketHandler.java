package com.futures.market.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futures.market.service.MarketService;
import com.futures.market.service.OmdDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行情 WebSocket 处理器
 *
 * <p>接收客户端订阅请求，推送实时行情数据。
 * 支持按合约代码订阅/取消订阅。</p>
 */
@Slf4j
@Component
public class MarketWebSocketHandler extends TextWebSocketHandler {

    private final MarketService marketService;
    private final ObjectMapper objectMapper;

    /** 已连接的会话 */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    /** 会话 → 订阅的合约列表 */
    private final Map<WebSocketSession, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public MarketWebSocketHandler(MarketService marketService, ObjectMapper objectMapper) {
        this.marketService = marketService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 连接建立: sessionId={}, 当前连接数={}",
                session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String action = (String) msg.getOrDefault("action", "");

            switch (action) {
                case "subscribe" -> {
                    @SuppressWarnings("unchecked")
                    List<String> symbols = (List<String>) msg.get("symbols");
                    if (symbols != null) {
                        subscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet())
                                .addAll(symbols);
                        log.info("订阅行情: session={}, symbols={}", session.getId(), symbols);
                        session.sendMessage(new TextMessage(
                                "{\"type\":\"subscribed\",\"symbols\":" + objectMapper.writeValueAsString(symbols) + "}"));
                    }
                }
                case "unsubscribe" -> {
                    @SuppressWarnings("unchecked")
                    List<String> symbols = (List<String>) msg.get("symbols");
                    if (symbols != null && subscriptions.containsKey(session)) {
                        subscriptions.get(session).removeAll(symbols);
                    }
                }
                case "ping" ->
                    session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                default ->
                    log.warn("未知消息类型: {}", action);
            }
        } catch (Exception e) {
            log.error("消息解析失败: {}", payload, e);
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"msg\":\"消息格式错误\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        subscriptions.remove(session);
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 广播行情数据给已订阅的客户端
     */
    public void broadcast(String symbol, String jsonData) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            Set<String> subs = subscriptions.get(session);
            if (subs != null && !subs.contains(symbol)) continue;

            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(jsonData));
                }
            } catch (IOException e) {
                log.error("推送行情失败: session={}, symbol={}", session.getId(), symbol, e);
            }
        }
    }

    /**
     * 生成行情推送 JSON
     */
    public String buildMarketPush(String symbol, BigDecimal last, BigDecimal bid, BigDecimal ask,
                                  BigDecimal high, BigDecimal low, long volume) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "market",
                    "symbol", symbol,
                    "last", last,
                    "bid", bid,
                    "ask", ask,
                    "high", high,
                    "low", low,
                    "volume", volume,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 生成盘口深度推送 JSON
     */
    public String buildDepthPush(String symbol, Object depth) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "depth",
                    "symbol", symbol,
                    "data", depth,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
