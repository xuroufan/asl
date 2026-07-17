package com.futures.market.service;

import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.*;
import com.futures.market.service.OmdDecoder;
import com.futures.market.service.MarketDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 行情数据服务 — 模拟 GBM 价格生成 + 实时报价管理
 *
 * <p>使用简化版几何布朗运动 (GBM) 模拟期货价格。</p>
 * <ul>
 *   <li>每个合约独立的漂移率 μ 和波动率 σ</li>
 *   <li>开盘跳空：每日第一次 step 时随机 ±1%</li>
 *   <li>自动生成五档盘口和 K 线数据</li>
 * </ul>
 */
@Slf4j
@Service
public class MarketService {

    // ─── 合约基本配置 ───
    private static final List<Map<String, Object>> SYMBOLS = List.of(
            // ── 港交所 (HKEX) ──
            Map.of("symbol", "HSI", "name", "恒生指数期货", "exchange", "HKEX", "multiplier", 50, "minTick", new BigDecimal("1")),
            Map.of("symbol", "HHI", "name", "H股指数期货",    "exchange", "HKEX", "multiplier", 50, "minTick", new BigDecimal("0.5")),
            Map.of("symbol", "MHI", "name", "小型恒指期货",   "exchange", "HKEX", "multiplier", 10, "minTick", new BigDecimal("1")),
            Map.of("symbol", "MCH", "name", "小型H股指数期货", "exchange", "HKEX", "multiplier", 10, "minTick", new BigDecimal("0.5")),
            Map.of("symbol", "HTI", "name", "恒生科技指数期货", "exchange", "HKEX", "multiplier", 50, "minTick", new BigDecimal("0.5")),
            // ── CME Group (US) ──
            Map.of("symbol", "ES", "name", "E-mini S&P 500",   "exchange", "CME", "multiplier", 50,  "minTick", new BigDecimal("0.25")),
            Map.of("symbol", "NQ", "name", "E-mini NASDAQ 100","exchange", "CME", "multiplier", 20,  "minTick", new BigDecimal("0.25")),
            Map.of("symbol", "GC", "name", "Gold Futures",      "exchange", "COMEX", "multiplier", 100, "minTick", new BigDecimal("0.1")),
            Map.of("symbol", "CL", "name", "Crude Oil Futures", "exchange", "NYMEX", "multiplier", 1000,"minTick", new BigDecimal("0.01")),
            Map.of("symbol", "6E", "name", "Euro FX Futures",   "exchange", "CME", "multiplier", 125000,"minTick", new BigDecimal("0.0001"))
    );

    /** 初始价格映射 */
    private static final Map<String, BigDecimal> INITIAL_PRICES;
    static {
        Map<String, BigDecimal> p = new HashMap<>();
        p.put("HSI", new BigDecimal("18500.00"));
        p.put("HHI", new BigDecimal("6200.00"));
        p.put("MHI", new BigDecimal("18500.00"));
        p.put("MCH", new BigDecimal("6200.00"));
        p.put("HTI", new BigDecimal("3800.00"));
        p.put("ES",  new BigDecimal("4500.00"));
        p.put("NQ",  new BigDecimal("15000.00"));
        p.put("GC",  new BigDecimal("2050.00"));
        p.put("CL",  new BigDecimal("78.00"));
        p.put("6E",  new BigDecimal("1.0900"));
        INITIAL_PRICES = Collections.unmodifiableMap(p);
    }

    // ─── 运行时状态 ───

    /** 行情状态 */
    private final Map<String, QuoteState> quotes = new ConcurrentHashMap<>();
    /** K线历史 */
    private final Map<String, List<KlineEntity>> klineHistory = new ConcurrentHashMap<>();

    /** GBM 参数 */
    private static final double MU = 0.05;          // 年化漂移率
    private static final double SIGMA = 0.20;        // 年化波动率
    private static final double DT = 1.0 / (365 * 24 * 60); // 每分钟时间步长

    private final Random random = new Random();

    /** OMD 解码器（用于生成 OMD 格式行情快照） */
    @org.springframework.beans.factory.annotation.Autowired
    private OmdDecoder omdDecoder;

    @Autowired(required = false)
    private MarketDataProvider marketDataProvider;

    private boolean useProvider = false;

    @PostConstruct
    public void checkProvider() {
        if (marketDataProvider != null && marketDataProvider.isAvailable()) {
            useProvider = true;
            log.info("iTick 行情提供者就绪，将使用真实行情数据");
        }
    }

    public void setOmdDecoder(OmdDecoder omdDecoder) {
        this.omdDecoder = omdDecoder;
    }

    /**
     * 获取 OMD 格式的行情快照 — 港交所 OMD 协议兼容格式。
     *
     * @param symbol 合约代码
     * @return OMD 格式行情数据
     */
    public Map<String, Object> getOmdSnapshot(String symbol) {
        if (omdDecoder == null) return Map.of("type", "ERROR", "error", "OMD 解码器未注入");
        QuoteState qs = quotes.get(symbol);
        if (qs == null) return Map.of("type", "ERROR", "error", "合约不存在");
        byte[] omdRaw = omdDecoder.generateMockSnapshot(symbol, qs.last);
        return omdDecoder.decode(omdRaw);
    }

    /** 获取所有合约的 OMD 格式快照。 */
    public Map<String, Map<String, Object>> getAllOmdSnapshots() {
        Map<String, Map<String, Object>> all = new LinkedHashMap<>();
        for (String sym : getSymbolList()) {
            all.put(sym, getOmdSnapshot(sym));
        }
        return all;
    }

    /** 合约配置索引 */
    private final Map<String, Map<String, Object>> symbolIndex = new ConcurrentHashMap<>();

    /** 每日是否已开盘跳空 */
    private boolean dailyGapApplied = false;

    @PostConstruct
    public void init() {
        for (Map<String, Object> sym : SYMBOLS) {
            String symbol = (String) sym.get("symbol");
            symbolIndex.put(symbol, sym);
            BigDecimal initPrice = INITIAL_PRICES.getOrDefault(symbol, new BigDecimal("10000"));
            quotes.put(symbol, new QuoteState(initPrice));
            klineHistory.put(symbol, new CopyOnWriteArrayList<>());
        }
        log.info("行情服务初始化完成，共 {} 个合约", SYMBOLS.size());
    }

    // ─── 公开方法 ───

    /** 获取所有合约列表 */
    public List<Map<String, Object>> getSymbols() {
        if (useProvider) {
            try { return marketDataProvider.getSymbols(); }
            catch (Exception e) { log.debug("行情API获取symbols失败，使用模拟数据"); }
        }
        return SYMBOLS;
    }

    /** 获取所有合约代码列表 */
    public List<String> getSymbolList() {
        if (useProvider) {
            try {
                return marketDataProvider.getSymbols().stream()
                    .map(m -> (String) m.get("symbol"))
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) { /* fallback */ }
        }
        return SYMBOLS.stream()
                .map(m -> (String) m.get("symbol"))
                .toList();
    }

    /** 推进指定合约价格（GBM 模拟） */
    public void stepPrice(String symbol) {
        QuoteState qs = quotes.get(symbol);
        if (qs == null) return;

        synchronized (qs) {
            BigDecimal oldPrice = qs.last;

            // 开盘跳空（每天第一次 step）
            if (!dailyGapApplied) {
                double gap = (random.nextDouble() - 0.5) * 0.02; // ±1%
                qs.last = qs.last.multiply(BigDecimal.valueOf(1 + gap));
                dailyGapApplied = true;
            }

            // GBM 步进
            double Z = random.nextGaussian();
            double drift = MU * DT;
            double diffusion = SIGMA * Math.sqrt(DT) * Z;
            double returnRate = drift + diffusion;

            BigDecimal newPrice = qs.last.multiply(BigDecimal.valueOf(1 + returnRate))
                    .setScale(2, RoundingMode.HALF_UP);

            qs.last = newPrice;
            qs.high = qs.high.max(newPrice);
            qs.low = qs.low.min(newPrice);
            qs.volume += random.nextInt(50) + 1;

            // 模拟 Bid/Ask Spread
            BigDecimal spread = newPrice.multiply(new BigDecimal("0.0002")).setScale(2, RoundingMode.HALF_UP);
            qs.bid = newPrice.subtract(spread);
            qs.ask = newPrice.add(spread);

            // 更新 K 线
            updateKline(symbol, qs);
        }
    }

    /** 获取实时报价 */
    public Map<String, Object> getQuote(String symbol) {
        if (useProvider) {
            try {
                Map<String, Object> realQuote = marketDataProvider.getQuote(symbol);
                if (realQuote != null) return realQuote;
            } catch (Exception e) { log.debug("行情API获取{}报价失败，使用模拟数据", symbol); }
        }
        QuoteState qs = quotes.get(symbol);
        if (qs == null) return null;

        Map<String, Object> quote = new LinkedHashMap<>();
        quote.put("symbol", symbol);
        quote.put("bid", qs.bid);
        quote.put("ask", qs.ask);
        quote.put("last", qs.last);
        quote.put("high", qs.high);
        quote.put("low", qs.low);
        quote.put("volume", qs.volume);
        quote.put("change", qs.last.subtract(qs.open));
        quote.put("changePercent", qs.open.compareTo(BigDecimal.ZERO) > 0
                ? qs.last.subtract(qs.open).multiply(new BigDecimal("100"))
                .divide(qs.open, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        quote.put("timestamp", System.currentTimeMillis());
        return quote;
    }

    /** 获取五档盘口 */
    public MarketDepth getDepth(String symbol) {
        if (useProvider) {
            try { return marketDataProvider.getDepth(symbol); }
            catch (Exception e) { /* fallback */ }
        }
        QuoteState qs = quotes.get(symbol);
        if (qs == null) return null;

        MarketDepth depth = new MarketDepth();
        depth.setSymbol(symbol);
        depth.setBids(new ArrayList<>());
        depth.setAsks(new ArrayList<>());

        BigDecimal minTick = (BigDecimal) symbolIndex.getOrDefault(symbol, Map.of())
                .getOrDefault("minTick", new BigDecimal("0.5"));

        // 生成五档买盘
        for (int i = 0; i < 5; i++) {
            MarketDepth.Level level = new MarketDepth.Level();
            BigDecimal price = qs.bid.subtract(minTick.multiply(BigDecimal.valueOf(i)));
            level.setPosition(i + 1);
            level.setPrice(price);
            level.setQuantity(BigDecimal.valueOf(random.nextInt(200) + 10));
            depth.getBids().add(level);
        }

        // 生成五档卖盘
        for (int i = 0; i < 5; i++) {
            MarketDepth.Level level = new MarketDepth.Level();
            BigDecimal price = qs.ask.add(minTick.multiply(BigDecimal.valueOf(i)));
            level.setPosition(i + 1);
            level.setPrice(price);
            level.setQuantity(BigDecimal.valueOf(random.nextInt(200) + 10));
            depth.getAsks().add(level);
        }

        return depth;
    }

    /** 获取 K 线数据 */
    public List<KlineEntity> getKlines(String symbol, String interval, int limit) {
        if (useProvider) {
            try { return marketDataProvider.getKlines(symbol, interval, limit); }
            catch (Exception e) { log.debug("获取{}K线失败，使用模拟数据", symbol); }
        }
        List<KlineEntity> all = klineHistory.get(symbol);
        if (all == null || all.isEmpty()) {
            // 无数据时生成一些历史 K 线
            return generateSampleKlines(symbol, interval, limit);
        }
        int size = all.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(all.subList(from, size));
    }

    /** 全部合约的报价快照 */
    public Map<String, Map<String, Object>> getAllQuotes() {
        Map<String, Map<String, Object>> all = new LinkedHashMap<>();
        for (String sym : getSymbolList()) {
            all.put(sym, getQuote(sym));
        }
        return all;
    }

    // ─── 内部方法 ───

    private void updateKline(String symbol, QuoteState qs) {
        List<KlineEntity> klines = klineHistory.get(symbol);
        if (klines == null) return;

        KlineEntity latest = klines.isEmpty() ? null : klines.get(klines.size() - 1);
        long now = System.currentTimeMillis();

        // 如果当前 K 线属于新的一分钟，创建新 K 线
        if (latest == null || (now - latest.getTimestamp()) >= 60_000) {
            KlineEntity kline = new KlineEntity();
            kline.setSymbol(symbol);
            kline.setInterval("1m");
            kline.setOpen(qs.last);
            kline.setHigh(qs.last);
            kline.setLow(qs.last);
            kline.setClose(qs.last);
            kline.setVolume(0L);
            kline.setTimestamp(now - (now % 60_000)); // 对齐到整分钟
            klines.add(kline);
        } else {
            latest.setHigh(latest.getHigh().max(qs.last));
            latest.setLow(latest.getLow().min(qs.last));
            latest.setClose(qs.last);
            latest.setVolume(latest.getVolume() + 1);
        }

        // 限制 K 线数量，防止内存溢出
        if (klines.size() > 10000) {
            klines.subList(0, 1000).clear();
        }
    }

    private List<KlineEntity> generateSampleKlines(String symbol, String interval, int limit) {
        List<KlineEntity> samples = new ArrayList<>();
        BigDecimal price = INITIAL_PRICES.getOrDefault(symbol, new BigDecimal("10000"));
        long baseTs = System.currentTimeMillis() - (limit * 60_000L);

        for (int i = 0; i < limit; i++) {
            KlineEntity k = new KlineEntity();
            k.setSymbol(symbol);
            k.setInterval(interval);
            k.setOpen(price);

            double Z1 = random.nextGaussian();
            double Z2 = random.nextGaussian();
            double ret = MU * DT + SIGMA * Math.sqrt(DT) * Z1;

            BigDecimal high = price.multiply(BigDecimal.valueOf(1 + Math.abs(Z2) * 0.003));
            BigDecimal low = price.multiply(BigDecimal.valueOf(1 - Math.abs(Z2) * 0.003));

            k.setHigh(high);
            k.setLow(low);
            k.setClose(price.multiply(BigDecimal.valueOf(1 + ret)).setScale(2, RoundingMode.HALF_UP));
            k.setVolume((long) (random.nextInt(1000) + 100));
            k.setTimestamp(baseTs + (i * 60_000L));

            samples.add(k);
            price = k.getClose();
        }
        return samples;
    }

    // ─── 内部状态类 ───

    private static class QuoteState {
        BigDecimal last;
        BigDecimal bid;
        BigDecimal ask;
        BigDecimal high;
        BigDecimal low;
        BigDecimal open;
        long volume;

        QuoteState(BigDecimal initial) {
            this.last = initial;
            this.bid = initial.subtract(initial.multiply(new BigDecimal("0.0002")));
            this.ask = initial.add(initial.multiply(new BigDecimal("0.0002")));
            this.high = initial;
            this.low = initial;
            this.open = initial;
            this.volume = 0;
        }
    }

    /**
     * 获取最近成交记录
     * @param symbol 合约代码
     * @param limit 返回条数
     * @return 成交记录列表
     */
    public List<Map<String, Object>> getTrades(String symbol, int limit) {
        if (useProvider) {
            try { return marketDataProvider.getTrades(symbol, limit); }
            catch (Exception e) { /* fallback */ }
        }
        QuoteState qs = quotes.get(symbol);
        if (qs == null) return List.of();
        List<Map<String, Object>> trades = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = limit; i > 0; i--) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", "t-" + symbol + "-" + (now - i * 500));
            t.put("time", java.time.Instant.ofEpochMilli(now - i * 500).toString());
            double noise = (random.nextDouble() - 0.5) * 0.002;
            BigDecimal price = qs.last.multiply(BigDecimal.valueOf(1 + noise))
                .setScale(2, RoundingMode.HALF_UP);
            t.put("price", price);
            t.put("volume", random.nextInt(10) + 1);
            t.put("side", random.nextBoolean() ? "BUY" : "SELL");
            trades.add(t);
        }
        return trades;
    }

}
