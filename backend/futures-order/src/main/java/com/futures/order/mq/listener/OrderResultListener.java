package com.futures.order.mq.listener;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futures.common.exception.BizException;
import com.futures.order.dto.OrderMatchEvent;
import com.futures.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 撮合成交结果消费者
 * <p>
 * 消费撮合引擎（Matching Engine）发送的成交结果消息。
 * Topic: <b>order-matched</b>
 * <p>
 * 处理流程：
 * <ol>
 *   <li>反序列化消息体为 {@link OrderMatchEvent}</li>
 *   <li>调用 {@link OrderService#handleMatchEvent(OrderMatchEvent)} 更新订单状态</li>
 *   <li>处理完毕自动 ACK（RocketMQListener 默认自动提交）</li>
 * </ol>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "order-matched",
        consumerGroup = "order-consumer-group",
        selectorExpression = "*"
)
public class OrderResultListener implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OrderService orderService;

    @Override
    public void onMessage(MessageExt message) {
        String msgId = message.getMsgId();
        String body = new String(message.getBody());
        log.info("收到撮合成交消息: msgId={}, body={}, tags={}, keys={}",
                msgId, body, message.getTags(), message.getKeys());

        try {
            // 1. 反序列化消息体
            OrderMatchEvent event = OBJECT_MAPPER.readValue(body, OrderMatchEvent.class);
            if (event.getOrderId() == null || event.getOrderId().isBlank()) {
                log.warn("成交消息中 orderId 为空: msgId={}", msgId);
                return;
            }

            // 2. 调用订单服务处理成交事件
            orderService.handleMatchEvent(event);

            log.info("成交处理成功: orderId={}, newStatus={}, filledVolume={}, price={}",
                    event.getOrderId(), event.getNewStatus(),
                    event.getTotalFilledVolume(), event.getAvgPrice());

        } catch (BizException e) {
            // 已知业务异常（如状态转移非法），不重试
            log.warn("成交消息业务处理失败（不重试）: msgId={}, orderId={}, reason={}",
                    msgId,
                    extractOrderId(body),
                    e.getMessage());
        } catch (Exception e) {
            // 非预期异常，抛出 RuntimeException 触发 RocketMQ 重试
            log.error("成交消息处理异常: msgId={}, body={}", msgId, body, e);
            throw new RuntimeException("成交消息处理失败: " + e.getMessage(), e);
        }
    }

    /** 从 JSON body 中快速提取 orderId */
    private String extractOrderId(String body) {
        try {
            return OBJECT_MAPPER.readTree(body).path("orderId").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
