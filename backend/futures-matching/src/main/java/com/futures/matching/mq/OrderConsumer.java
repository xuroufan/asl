package com.futures.matching.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futures.matching.disruptor.DisruptorConfig;
import com.futures.matching.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * RocketMQ 订单消息消费者。
 * <p>
 * 消费 Order Service 发布的下单事件，路由到对应 Symbol 的 Disruptor 进行撮合。
 * </p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-created",
        consumerGroup = "matching-consumer-group"
)
public class OrderConsumer implements RocketMQListener<String> {

    private final DisruptorConfig disruptorConfig;
    private final ObjectMapper objectMapper;

    public OrderConsumer(DisruptorConfig disruptorConfig) {
        this.disruptorConfig = disruptorConfig;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);

            String symbol = (String) msg.get("symbol");
            String direction = (String) msg.get("direction");
            String type = (String) msg.get("orderType");
            Number userIdNum = (Number) msg.get("userId");
            Number volumeNum = (Number) msg.get("volume");

            if (symbol == null || direction == null || type == null) {
                log.warn("无效订单消息: {}", message);
                return;
            }

            long userId = userIdNum != null ? userIdNum.longValue() : 0L;
            int volume = volumeNum != null ? volumeNum.intValue() : 0;
            if (volume <= 0) return;

            Order.Direction dir;
            try {
                dir = Order.Direction.valueOf(direction.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("未知方向: direction={}", direction);
                return;
            }

            Order.OrderType orderType;
            try {
                orderType = Order.OrderType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("未知订单类型: type={}", type);
                return;
            }

            long price = 0;
            Object priceObj = msg.get("price");
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).longValue();
            }

            // 创建撮合引擎内部订单
            Order order = Order.builder()
                    .orderId("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .userId(String.valueOf(userId))
                    .direction(dir)
                    .type(orderType)
                    .price(price)
                    .volume(volume)
                    .filledVolume(0)
                    .timestamp(System.nanoTime())
                    .build();

            // 异步发布到 Disruptor
            disruptorConfig.publishOrder(order);

            log.info("接收订单消息 -> 送撮合: orderId={}, symbol={}, direction={}, type={}, volume={}",
                    order.getOrderId(), symbol, direction, orderType, volume);

        } catch (Exception e) {
            log.error("处理订单消息异常: {}", message, e);
        }
    }
}
