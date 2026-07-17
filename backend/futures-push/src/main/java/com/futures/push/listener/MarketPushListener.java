package com.futures.push.listener;

import com.futures.common.message.RocketMQTopic;
import com.futures.common.message.event.MarketTickEvent;
import com.futures.push.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 行情Tick消息推送监听器。
 *
 * <p>消费 {@code futures-market-tick} Topic，将实时行情数据通过 WebSocket
 * 推送给订阅了对应合约的用户终端。
 *
 * <p>推送格式：
 * <pre>{@code
 * {
 *   "type": "market",
 *   "symbol": "HSI2309",
 *   "data": {
 *     "lastPrice": 18500.50,
 *     "bidPrice": 18499.50,
 *     "askPrice": 18501.50,
 *     "bidVolume": 12,
 *     "askVolume": 8,
 *     "openPrice": 18450.00,
 *     "highPrice": 18520.00,
 *     "lowPrice": 18420.00,
 *     "volume": 12340,
 *     "changePercent": 0.27,
 *     "timestamp": "2026-07-12T10:30:00"
 *   }
 * }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQTopic.MARKET_TICK,
        consumerGroup = "push-market-consumer"
)
public class MarketPushListener implements RocketMQListener<MarketTickEvent> {

    private final PushService pushService;

    @Override
    public void onMessage(MarketTickEvent event) {
        String symbol = event.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            log.warn("行情Tick缺少合约代码，跳过推送");
            return;
        }

        Map<String, Object> pushData = new LinkedHashMap<>();
        pushData.put("type", "market");
        pushData.put("symbol", symbol);
        pushData.put("data", buildMarketData(event));

        pushService.pushToMarketSubscribers(symbol, pushData);
    }

    private Map<String, Object> buildMarketData(MarketTickEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lastPrice", safeDecimal(event.getLastPrice()));
        data.put("bidPrice", safeDecimal(event.getBidPrice()));
        data.put("bidVolume", event.getBidVolume());
        data.put("askPrice", safeDecimal(event.getAskPrice()));
        data.put("askVolume", event.getAskVolume());
        data.put("openPrice", safeDecimal(event.getOpenPrice()));
        data.put("highPrice", safeDecimal(event.getHighPrice()));
        data.put("lowPrice", safeDecimal(event.getLowPrice()));
        data.put("preClosePrice", safeDecimal(event.getPreClosePrice()));
        data.put("volume", event.getVolume());
        data.put("turnover", safeDecimal(event.getTurnover()));
        data.put("openInterest", safeDecimal(event.getOpenInterest()));
        data.put("changePercent", safeDecimal(event.getChangePercent()));
        data.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        return data;
    }

    private static BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
