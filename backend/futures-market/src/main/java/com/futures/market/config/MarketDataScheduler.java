package com.futures.market.config;

import com.futures.market.service.MarketService;
import com.futures.market.service.OmdDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行情数据定时调度 — 模拟 OMD 数据生成 + WebSocket 广播
 *
 * <p>每 3 秒推进一次行情（模拟 1 分钟 Tick），使用 OmdDecoder 生成
 * 模拟的 OMD 协议行情数据，并通过 WebSocket 推送给已订阅的客户端。</p>
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MarketDataScheduler {

    private final MarketService marketService;
    private final MarketWebSocketHandler wsHandler;
    private final OmdDecoder omdDecoder;

    /** 上一轮广播的价格缓存（用于判断价格变化） */
    private final Map<String, BigDecimal> lastBroadcastPrice = new ConcurrentHashMap<>();

    /**
     * 每 3 秒推进行情 → 相当于模拟 1 分钟 Tick 间隔
     */
    @Scheduled(fixedRate = 3000)
    public void stepMarketData() {
        for (String symbol : marketService.getSymbolList()) {
            try {
                // 1. 推进价格（GBM 模拟）
                marketService.stepPrice(symbol);

                // 2. 获取最新报价
                Map<String, Object> quote = marketService.getQuote(symbol);
                if (quote == null) continue;

                BigDecimal last = (BigDecimal) quote.get("last");
                BigDecimal bid = (BigDecimal) quote.get("bid");
                BigDecimal ask = (BigDecimal) quote.get("ask");
                BigDecimal high = (BigDecimal) quote.get("high");
                BigDecimal low = (BigDecimal) quote.get("low");
                long volume = ((Number) quote.get("volume")).longValue();

                // 3. 通过 OmdDecoder 生成标准 OMD 格式消息
                byte[] omdRaw = omdDecoder.generateMockSnapshot(symbol, last);

                // 4. 构建 JSON 推送
                String jsonData = wsHandler.buildMarketPush(
                        symbol, last, bid, ask, high, low, volume);

                // 5. WebSocket 广播（行情数据）
                wsHandler.broadcast(symbol, jsonData);

                // 6. 广播盘口深度数据
                try {
                    var depth = marketService.getDepth(symbol);
                    if (depth != null) {
                        String depthJson = wsHandler.buildDepthPush(symbol, depth);
                        wsHandler.broadcast(symbol + ":depth", depthJson);
                    }
                } catch (Exception ignored) {}

                // 仅变化时记录日志
                BigDecimal prev = lastBroadcastPrice.get(symbol);
                if (prev == null || prev.compareTo(last) != 0) {
                    lastBroadcastPrice.put(symbol, last);
                }

            } catch (Exception e) {
                log.warn("行情步进失败: symbol={}", symbol, e);
            }
        }
    }
}
