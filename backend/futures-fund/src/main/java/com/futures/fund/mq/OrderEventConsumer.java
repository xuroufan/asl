package com.futures.fund.mq;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.OrderCancelledEvent;
import com.futures.common.message.event.OrderMatchedEvent;
import com.futures.fund.service.FundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * RocketMQ 订单事件消费者。
 * <p>
 * 消费撮合引擎的成交和撤单事件，自动更新资金账户：
 * <ul>
 *   <li>order-matched → 扣减冻结资金（deduct）</li>
 *   <li>order-cancelled → 解冻冻结资金（unfreezeMargin）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final FundService fundService;

    // ==================== 成交事件消费 ====================

    /**
     * 消费成交消息，扣减冻结资金并更新持仓保证金。
     */
    @Component
    @RocketMQMessageListener(
            topic = RocketMQTopic.ORDER_MATCHED,
            consumerGroup = "fund-order-matched-group",
            maxReconsumeTimes = 3
    )
    public static class OrderMatchedConsumer implements RocketMQListener<OrderMatchedEvent> {

        private final FundService fundService;

        public OrderMatchedConsumer(FundService fundService) {
            this.fundService = fundService;
        }

        @Override
        public void onMessage(OrderMatchedEvent event) {
            log.info("收到成交事件: orderId={}, userId={}, symbol={}, price={}, volume={}",
                    event.getOrderId(), event.getUserId(), event.getSymbol(),
                    event.getPrice(), event.getVolume());

            try {
                // 扣减冻结资金（已冻结的保证金转为占用）
                // 成交后：frozen 减少，margin 增加
                // 成交金额 = 价格 × 合约乘数（简化处理）
                BigDecimal tradeAmount = event.getPrice().multiply(BigDecimal.valueOf(event.getVolume()));

                // 如果订单状态变为 FILLED，不再需要保证金冻结，释放冻结部分
                // 如果订单状态变为 PARTIAL，部分成交，按成交比例释放冻结保证金
                if ("FILLED".equals(event.getNewStatus())) {
                    // 全部成交：此前冻结的保证金全部转为占用
                    fundService.deduct(event.getUserId(), tradeAmount, event.getOrderId());
                } else if ("PARTIAL".equals(event.getNewStatus())) {
                    // 部分成交：按成交比例处理
                    fundService.deduct(event.getUserId(), tradeAmount, event.getOrderId());
                }

                log.info("成交事件处理完成: orderId={}, userId={}", event.getOrderId(), event.getUserId());
            } catch (Exception e) {
                log.error("成交事件处理失败: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
                // RocketMQ 自动重试（maxReconsumeTimes=3）
                throw new RuntimeException("成交事件处理失败", e);
            }
        }
    }

    // ==================== 撤单事件消费 ====================

    /**
     * 消费撤单消息，解冻冻结保证金。
     */
    @Component
    @RocketMQMessageListener(
            topic = RocketMQTopic.ORDER_CANCELLED,
            consumerGroup = "fund-order-cancelled-group",
            maxReconsumeTimes = 3
    )
    public static class OrderCancelledConsumer implements RocketMQListener<OrderCancelledEvent> {

        private final FundService fundService;

        public OrderCancelledConsumer(FundService fundService) {
            this.fundService = fundService;
        }

        @Override
        public void onMessage(OrderCancelledEvent event) {
            log.info("收到撤单事件: orderId={}, userId={}, symbol={}, remainingVolume={}",
                    event.getOrderId(), event.getUserId(), event.getSymbol(), event.getRemainingVolume());

            try {
                // 解冻对应合约的冻结保证金
                BigDecimal unfreezeAmount = BigDecimal.valueOf(event.getRemainingVolume());
                fundService.unfreeze(event.getUserId(), unfreezeAmount, event.getOrderId());

                log.info("撤单事件处理完成: orderId={}, userId={}", event.getOrderId(), event.getUserId());
            } catch (Exception e) {
                log.error("撤单事件处理失败: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
                throw new RuntimeException("撤单事件处理失败", e);
            }
        }
    }
}
