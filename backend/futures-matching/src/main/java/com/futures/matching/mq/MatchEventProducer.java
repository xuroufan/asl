package com.futures.matching.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.futures.matching.engine.MatchedOrder;
import com.futures.matching.model.MatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 撮合结果事件生产者。
 * <p>
 * 将 {@link MatchedOrder} 中的成交记录发送到 MQ。
 * </p>
 */
@Slf4j
@Component
public class MatchEventProducer {

    private final ObjectMapper objectMapper;

    @Value("${matching.mq.enabled:false}")
    private boolean mqEnabled;

    public MatchEventProducer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 发布撮合结果。
     */
    public void publishMatchResult(MatchedOrder matchedOrder) {
        if (!mqEnabled) {
            if (matchedOrder.isHasTrades()) {
                log.debug("MQ 未启用, 跳过发布: orderId={}, trades={}",
                        matchedOrder.getOrderId(), matchedOrder.getTrades().size());
            }
            return;
        }

        try {
            String orderId = matchedOrder.getOrderId();
            String symbol = matchedOrder.getSymbol();
            String finalStatus = matchedOrder.getFinalStatus();
            int totalVolume = matchedOrder.getTotalFilledVolume();

            // 1. 发送成交记录（逐笔）
            if (matchedOrder.isHasTrades()) {
                for (MatchResult trade : matchedOrder.getTrades()) {
                    publishOrderMatchedEvent(orderId, symbol, trade, finalStatus, totalVolume);
                }
            }

            // 2. 发送订单状态变更
            publishOrderStatusChangedEvent(matchedOrder, finalStatus);

            // 3. 发送持仓变化（仅在成交时）
            if (matchedOrder.isHasTrades()) {
                publishPositionChangedEvent(matchedOrder);
            }

        } catch (Exception e) {
            log.error("发布撮合结果异常: orderId={}", matchedOrder.getOrderId(), e);
        }
    }

    private void publishOrderMatchedEvent(String orderId, String symbol,
                                           MatchResult trade, String finalStatus,
                                           int totalVolume) throws Exception {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderId", orderId);
        msg.put("symbol", symbol);
        msg.put("matchPrice", trade.getPriceAsBigDecimal());
        msg.put("matchVolume", trade.getVolume());
        msg.put("totalFilledVolume", totalVolume);
        msg.put("avgPrice", null);
        msg.put("newStatus", finalStatus);
        msg.put("matchedAt", LocalDateTime.now().toString());
        msg.put("takerOrderId", trade.getTakerOrderId());
        msg.put("makerOrderId", trade.getMakerOrderId());

        String json = objectMapper.writeValueAsString(msg);
        log.info("[MQ] order-matched: {}", json);
    }

    private void publishOrderStatusChangedEvent(MatchedOrder matchedOrder,
                                                 String finalStatus) throws Exception {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderId", matchedOrder.getOrderId());
        msg.put("symbol", matchedOrder.getSymbol());
        msg.put("oldStatus", "PENDING");
        msg.put("newStatus", finalStatus);
        msg.put("filledVolume", matchedOrder.getTotalFilledVolume());
        msg.put("remainingVolume", matchedOrder.getRemainingVolume());
        msg.put("avgPrice", matchedOrder.getAvgPrice());
        msg.put("timestamp", LocalDateTime.now().toString());

        String json = objectMapper.writeValueAsString(msg);
        log.info("[MQ] order-status-changed: {}", json);
    }

    private void publishPositionChangedEvent(MatchedOrder matchedOrder) throws Exception {
        String symbol = matchedOrder.getSymbol();
        for (MatchResult trade : matchedOrder.getTrades()) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", matchedOrder.getOrder() != null ? matchedOrder.getOrder().getUserId() : "0");
            msg.put("symbol", symbol);
            msg.put("volume", trade.getVolume());
            msg.put("price", trade.getPriceAsBigDecimal());
            msg.put("tradeId", matchedOrder.getOrderId());

            String json = objectMapper.writeValueAsString(msg);
            log.info("[MQ] position-changed: {}", json);
        }
    }
}
