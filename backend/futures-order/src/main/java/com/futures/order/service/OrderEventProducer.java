package com.futures.order.service;

import com.futures.order.dto.OrderMatchEvent;
import com.futures.order.dto.PlaceOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 订单事件生产者
 * <p>
 * 同时支持两种消息通道：
 * <ul>
 *   <li><b>进程内事件</b>（Spring {@link ApplicationEventPublisher}）：开发/测试环境使用，零依赖</li>
 *   <li><b>RocketMQ</b>：生产环境使用，保证消息持久化和跨服务可靠投递</li>
 * </ul>
 * <p>
 * 模式选择：通过 {@code rocketmq.enabled} 配置切换，默认使用进程内事件。
 * <p>
 * 发送的 Topic：
 * <ul>
 *   <li>{@code order-created} → 撮合引擎（Matching Engine）消耗，触发撮合</li>
 *   <li>{@code order-matched} → 订单服务自己消耗（{@link com.futures.order.mq.listener.OrderResultListener}），更新订单状态</li>
 * </ul>
 */
@Slf4j
@Service
public class OrderEventProducer {

    private final ApplicationEventPublisher eventPublisher;

    public OrderEventProducer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** RocketMQ 模板（可选注入：不启用 RocketMQ 时不创建该 Bean） */
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /** 是否启用 RocketMQ 生产环境模式 */
    @Value("${rocketmq.enabled:false}")
    private boolean rocketMqEnabled;

    // ==================== 下单事件 ====================

    /**
     * 发布下单事件给撮合引擎
     * <p>
     * 进程内模式：直接 publish {@link PlaceOrderEvent}，由 {@link com.futures.order.service.MatchingEventHandler} 消费
     * RocketMQ 模式：发送到 {@code order-created} Topic
     *
     * @param event 下单事件
     */
    public void publishPlaceOrder(PlaceOrderEvent event) {
        log.info("发布下单事件: orderId={}, symbol={}, direction={}, volume={}",
                event.getOrderId(), event.getSymbol(), event.getDirection(), event.getVolume());

        // 进程内事件（开发/测试环境）
        eventPublisher.publishEvent(event);

        // RocketMQ 生产环境消息
        if (rocketMqEnabled) {
            try {
                rocketMQTemplate.syncSend(
                        "order-created",
                        MessageBuilder.withPayload(event).build(),
                        3000 // 超时 3 秒
                );
                log.debug("下单事件已发送到 RocketMQ: topic=order-created, orderId={}", event.getOrderId());
            } catch (Exception e) {
                log.error("下单事件 RocketMQ 发送失败: orderId={}", event.getOrderId(), e);
            }
        }
    }

    // ==================== 撮合成交事件 ====================

    /**
     * 发布撮合成交事件（更新订单状态）
     * <p>
     * 进程内模式：直接 publish {@link OrderMatchEvent}
     * RocketMQ 模式：发送到 {@code order-matched} Topic
     *
     * @param event 成交事件
     */
    public void publishMatch(OrderMatchEvent event) {
        log.info("发布成交事件: orderId={}, matchVolume={}, price={}, newStatus={}",
                event.getOrderId(), event.getMatchVolume(), event.getMatchPrice(), event.getNewStatus());

        // 进程内事件
        eventPublisher.publishEvent(event);

        // RocketMQ 生产环境消息
        if (rocketMqEnabled) {
            try {
                rocketMQTemplate.syncSend(
                        "order-matched",
                        MessageBuilder.withPayload(event).build(),
                        3000
                );
                log.debug("成交事件已发送到 RocketMQ: topic=order-matched, orderId={}", event.getOrderId());
            } catch (Exception e) {
                log.error("成交事件 RocketMQ 发送失败: orderId={}", event.getOrderId(), e);
            }
        }
    }
}
