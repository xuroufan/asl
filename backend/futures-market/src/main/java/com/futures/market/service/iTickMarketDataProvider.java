package com.futures.market.service;

import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * iTick 行情数据提供者 — 对接 https://api-free.itick.io/future
 * 
 * 免费功能：
 * - 美股/期货实时行情（延时 15 分钟）
 * - K 线数据
 * - 基础报价
 * 
 * 使用方式：
 * 1. 在 https://itick.io 注册账号
 * 2. 在 Dashboard 中生成 API Key
 * 3. 配置到 application.yml: itick.api-key=xxx
 */
@Slf4j
@Service
public class iTickMarketDataProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private boolean available = false;

    @Value("${itick.api-key:}")
    private String apiKey;

    @Value("${itick.base-url:https://api-free.itick.io/future}")
    private String baseUrl;

    // 缓存
    private final Map<String, Map<String, Object>> quoteCache = new ConcurrentHashMap<>();
    private volatile long lastRefresh = 0;
    private static final long CACHE_TTL_MS = 10_000;  // 10秒刷新

    public iTickMarketDataProvider(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("iTick API key 未配置，行情将使用模拟数据。");
            log.warn("请到 https://itick.io 注册并获取 API Key，配置 itick.api-key=xxx");
            this.available = false;
            return;
        }
        log.info("iTick 行情提供者初始化完成 (baseUrl={})", baseUrl);
        // 尝试连接验证 API key
        try {
            Map<String, Object> resp = restTemplate.getForObject(
                    baseUrl + "/symbols?apikey={key}",
                    Map.class, apiKey);
            if (resp != null && resp.containsKey("code") && 
                ((Number) resp.get("code")).intValue() == 0) {
                this.available = true;
                log.info("iTick API 连接成功！将使用真实行情数据。");
            } else {
                log.warn("iTick API 返回异常: {}", resp);
            }
        } catch (Exception e) {
            log.warn("iTick API 连接失败: {}，将使用模拟数据。", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public List<Map<String, Object>> getSymbols() {
        try {
            Map<String, Object> resp = restTemplate.getForObject(
                    baseUrl + "/symbols?apikey={key}", Map.class, apiKey);
            if (resp != null && resp.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> symbols = (List<Map<String, Object>>) resp.get("data");
                log.debug("从 iTick 获取到 {} 个合约", symbols.size());
                return symbols;
            }
        } catch (Exception e) {
            log.debug("获取合约列表失败: {}", e.getMessage());
        }
        throw new RuntimeException("iTick symbols unavailable");
    }

    @Override
    public Map<String, Object> getQuote(String symbol) {
        // 检查缓存
        long now = System.currentTimeMillis();
        Map<String, Object> cached = quoteCache.get(symbol);
        if (cached != null && (now - lastRefresh) < CACHE_TTL_MS) {
            return cached;
        }

        try {
            Map<String, Object> resp = restTemplate.getForObject(
                    baseUrl + "/quote?symbol={symbol}&apikey={key}",
                    Map.class, symbol, apiKey);
            if (resp != null && resp.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> quote = (Map<String, Object>) resp.get("data");
                quoteCache.put(symbol, quote);
                lastRefresh = now;
                return quote;
            }
        } catch (Exception e) {
            log.debug("获取 {} 报价失败: {}", symbol, e.getMessage());
        }
        // 返回缓存（即使过期）
        if (cached != null) return cached;
        throw new RuntimeException("iTick quote unavailable for " + symbol);
    }

    @Override
    public MarketDepth getDepth(String symbol) {
        // iTick 免费版可能不提供深度数据
        // 根据报价模拟生成深度
        Map<String, Object> quote = getQuote(symbol);
        BigDecimal bid = new BigDecimal(quote.get("bid").toString());
        BigDecimal ask = new BigDecimal(quote.get("ask").toString());
        BigDecimal minTick = new BigDecimal("0.25");

        MarketDepth depth = new MarketDepth();
        depth.setSymbol(symbol);
        depth.setBids(new ArrayList<>());
        depth.setAsks(new ArrayList<>());

        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            MarketDepth.Level bidLevel = new MarketDepth.Level();
            bidLevel.setPosition(i + 1);
            bidLevel.setPrice(bid.subtract(minTick.multiply(BigDecimal.valueOf(i))));
            bidLevel.setQuantity(BigDecimal.valueOf(rand.nextInt(200) + 10));
            depth.getBids().add(bidLevel);

            MarketDepth.Level askLevel = new MarketDepth.Level();
            askLevel.setPosition(i + 1);
            askLevel.setPrice(ask.add(minTick.multiply(BigDecimal.valueOf(i))));
            askLevel.setQuantity(BigDecimal.valueOf(rand.nextInt(200) + 10));
            depth.getAsks().add(askLevel);
        }
        return depth;
    }

    @Override
    public List<KlineEntity> getKlines(String symbol, String interval, int limit) {
        try {
            Map<String, Object> resp = restTemplate.getForObject(
                    baseUrl + "/kline?symbol={symbol}&interval={interval}&limit={limit}&apikey={key}",
                    Map.class, symbol, interval, Math.min(limit, 500), apiKey);
            if (resp != null && resp.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> raw = (List<Map<String, Object>>) resp.get("data");
                return raw.stream().map(k -> {
                    KlineEntity e = new KlineEntity();
                    e.setSymbol(symbol);
                    e.setInterval(interval);
                    e.setOpen(new BigDecimal(k.get("open").toString()));
                    e.setHigh(new BigDecimal(k.get("high").toString()));
                    e.setLow(new BigDecimal(k.get("low").toString()));
                    e.setClose(new BigDecimal(k.get("close").toString()));
                    e.setVolume(k.get("volume") instanceof Number 
                        ? ((Number) k.get("volume")).longValue() : 0);
                    e.setTimestamp(k.get("time") instanceof Number
                        ? ((Number) k.get("time")).longValue() * 1000 : System.currentTimeMillis());
                    return e;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("获取 {} K线失败: {}", symbol, e.getMessage());
        }
        throw new RuntimeException("iTick klines unavailable for " + symbol);
    }

    @Override
    public List<Map<String, Object>> getTrades(String symbol, int limit) {
        try {
            Map<String, Object> resp = restTemplate.getForObject(
                    baseUrl + "/trades?symbol={symbol}&limit={limit}&apikey={key}",
                    Map.class, symbol, Math.min(limit, 100), apiKey);
            if (resp != null && resp.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> trades = (List<Map<String, Object>>) resp.get("data");
                return trades;
            }
        } catch (Exception e) {
            log.debug("获取 {} 成交记录失败: {}", symbol, e.getMessage());
        }
        throw new RuntimeException("iTick trades unavailable for " + symbol);
    }
}
