package com.futures.push.listener;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.OrderMatchedEvent;
import com.futures.push.service.PushService;
import com.futures.push.service.SubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单成交消息推送监听器。
 *
 * <p>消费 {@code futures-order-matched} Topic，将有订单状态变更的用户
 * 通过 WebSocket 实时通知终端。
 *
 * <p>推送格式：
 * <pre>{@code
 * {
 *   "type": "order",
 *   "data": {
 *     "orderId": "O20230712001",
 *     "symbol": "HSI2309",
 *     "direction": "BUY",
 *     "newStatus": "FILLED",
 *     "filledVolume": 2,
 *     "averagePrice": 18500.50,
 *     "matchedAt": "2026-07-12T10:30:00"
 *   }
 * }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQTopic.ORDER_MATCHED,
        consumerGroup = "push-order-consumer"
)
public class OrderPushListener implements RocketMQListener<OrderMatchedEvent> {

    private final PushService pushService;
    private final SubscriptionManager subscriptionManager;

    @Override
    public void onMessage(OrderMatchedEvent event) {
        String userId = event.getUserId();
        if (userId == null || userId.isBlank()) {
            log.warn("成交事件缺少 userId，跳过推送: orderId={}", event.getOrderId());
            return;
        }

        // 只推送已订阅订单状态更新的用户
        if (!subscriptionManager.isOrderSubscribed(userId)) {
            log.debug("用户 {} 未订阅订单推送，跳过: orderId={}", userId, event.getOrderId());
            return;
        }

        Map<String, Object> pushData = new LinkedHashMap<>();
        pushData.put("type", "order");
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("direction", event.getDirection());
        data.put("newStatus", event.getNewStatus());
        data.put("previousStatus", event.getPreviousStatus());
        data.put("price", event.getPrice());
        data.put("volume", event.getVolume());
        data.put("filledVolume", event.getTotalFilledVolume());
        data.put("averagePrice", event.getAveragePrice());
        data.put("totalAmount", event.getTotalAmount());
        data.put("fee", event.getFee());
        data.put("matchId", event.getMatchId());
        data.put("matchedAt", event.getMatchedAt() != null ? event.getMatchedAt().toString() : null);
        pushData.put("data", data);

        pushService.pushToUser(userId, pushData);
        log.debug("推送订单成交事件: userId={}, orderId={}, status={}",
                userId, event.getOrderId(), event.getNewStatus());
    }
}
