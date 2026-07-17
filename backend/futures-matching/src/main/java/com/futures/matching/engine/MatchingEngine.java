package com.futures.matching.engine;

import com.futures.matching.model.MatchResult;
import com.futures.matching.model.Order;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 撮合引擎 — 单品种内存撮合
 * <p>
 * 使用单线程设计，避免锁竞争。
 * 支持限价单、市价单、止损单、撤单。
 * 使用 {@link System#nanoTime()} 保证成交时间精度。
 */
@Slf4j
public class MatchingEngine {

    private final String symbol;
    private final OrderBook orderBook;

    /** 序列号生成器（用于成交ID） */
    private final AtomicLong matchSeq = new AtomicLong(0);

    /** 止损/止盈单阈值表：触发价 → 原始订单 */
    private final java.util.concurrent.ConcurrentSkipListMap<Long, List<Order>> stopOrders;

    public MatchingEngine(String symbol) {
        this.symbol = symbol;
        this.orderBook = new OrderBook();
        this.stopOrders = new java.util.concurrent.ConcurrentSkipListMap<>();
    }

    /**
     * 处理订单入口
     *
     * @param order 订单
     * @return 成交结果列表（可能为空）
     */
    public MatchedOrder processOrder(Order order) {
        long startNanos = System.nanoTime();

        List<MatchResult> results;
        switch (order.getType()) {
            case LIMIT:
                results = matchLimitOrder(order);
                break;
            case MARKET:
                results = matchMarketOrder(order);
                break;
            case STOP:
            case TAKE_PROFIT:
                results = handleStopOrder(order);
                break;
            case FOK:
                results = matchFokOrder(order);
                break;
            case IOC:
                results = matchIocOrder(order);
                break;
            default:
                log.warn("不支持的订单类型: {}", order.getType());
                results = Collections.emptyList();
        }

        long elapsed = System.nanoTime() - startNanos;
        log.debug("撮合耗时: {} ns, symbol={}, orderId={}, trades={}",
                elapsed, symbol, order.getOrderId(), results.size());

        return new MatchedOrder(order, results);
    }

    // ==================== 限价单撮合 ====================

    /**
     * 限价单撮合
     * <p>
     * 买单：从卖盘找价格 ≤ 买单价格的卖单，依次成交
     * 卖单：从买盘找价格 ≥ 卖单价格的买单，依次成交
     * 未成交部分挂入订单簿
     */
    public List<MatchResult> matchLimitOrder(Order order) {
        List<MatchResult> results = new ArrayList<>();

        if (order.isBuy()) {
            // 买单：从卖盘最低价开始，取价格 ≤ 买单价格的卖单
            NavigableMap<Long, Deque<Order>> eligibleAsks = orderBook.getAsksUpToPrice(order.getPrice());
            matchAgainstBook(order, eligibleAsks, false, results);
        } else {
            // 卖单：从买盘最高价开始，取价格 ≥ 卖单价格的买单
            NavigableMap<Long, Deque<Order>> eligibleBids = orderBook.getBidsUpToPrice(order.getPrice());
            matchAgainstBook(order, eligibleBids, true, results);
        }

        // 未成交部分挂入订单簿
        if (order.remainingVolume() > 0) {
            orderBook.add(order, order.isBuy());
        }

        return results;
    }

    // ==================== 市价单撮合 ====================

    /**
     * 市价单撮合
     * <p>
     * 以对手盘最优价立即成交，直至成交完毕或对手盘耗尽
     */
    public List<MatchResult> matchMarketOrder(Order order) {
        List<MatchResult> results = new ArrayList<>();

        if (order.isBuy()) {
            // 买单：与卖盘最低价成交
            NavigableMap<Long, Deque<Order>> asks = new java.util.TreeMap<>(orderBook.getAsksUpToPrice(Long.MAX_VALUE));
            matchAgainstBook(order, asks, false, results);
        } else {
            // 卖单：与买盘最高价成交
            NavigableMap<Long, Deque<Order>> bids = new java.util.TreeMap<>(orderBook.getBidsUpToPrice(Long.MAX_VALUE));
            matchAgainstBook(order, bids, true, results);
        }

        // 市价单剩余未成交部分直接丢弃（不做挂单处理）
        if (order.remainingVolume() > 0) {
            log.info("市价单未完全成交: orderId={}, remaining={}", order.getOrderId(), order.remainingVolume());
        }

        return results;
    }

    // ==================== FOK / IOC ====================

    /**
     * FOK（Fill or Kill）：不能全部成交则立即取消
     */
    public List<MatchResult> matchFokOrder(Order order) {
        // 先检查总可成交量
        int availableLiquidity = calculateAvailableLiquidity(order);
        if (availableLiquidity < order.getVolume()) {
            log.info("FOK单无法全部成交: orderId={}, available={}, required={}",
                    order.getOrderId(), availableLiquidity, order.getVolume());
            return Collections.emptyList();
        }
        return matchMarketOrder(toBuilder(order, Order.OrderType.LIMIT, order.getPrice()));
    }

    /**
     * IOC（Immediate or Cancel）：立即成交可成交部分，剩余取消
     */
    public List<MatchResult> matchIocOrder(Order order) {
        List<MatchResult> results = new ArrayList<>();

        if (order.isBuy()) {
            NavigableMap<Long, Deque<Order>> asks = new java.util.TreeMap<>(orderBook.getAsksUpToPrice(Long.MAX_VALUE));
            matchAgainstBook(order, asks, false, results);
        } else {
            NavigableMap<Long, Deque<Order>> bids = new java.util.TreeMap<>(orderBook.getBidsUpToPrice(Long.MAX_VALUE));
            matchAgainstBook(order, bids, true, results);
        }

        // IOC剩余部分直接丢弃
        return results;
    }

    // ==================== 止损/止盈单 ====================

    /**
     * 止损/止盈单处理
     * <p>
     * 先存入触发价位表，由行情Tick驱动检查。
     * 达到触发价时转为市价单立即撮合。
     */
    public List<MatchResult> handleStopOrder(Order order) {
        long triggerPrice = order.getPrice();
        stopOrders.compute(triggerPrice, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(order);
            return list;
        });
        log.info("止损单已注册: orderId={}, symbol={}, triggerPrice={}",
                order.getOrderId(), symbol, triggerPrice);
        return Collections.emptyList();
    }

    /**
     * 行情Tick驱动：检查止损/止盈单是否应触发
     *
     * @param lastPrice 当前最新价
     * @return 新产生的成交记录
     */
    public MatchedOrder onPriceTick(long lastPrice) {
        List<MatchResult> allResults = new ArrayList<>();

        // 取所有已达到触发条件的止损单
        NavigableMap<Long, List<Order>> triggered = stopOrders.headMap(lastPrice, true);
        if (triggered.isEmpty()) return new MatchedOrder(null, allResults);

        // 复制一份避免并发修改
        List<Map.Entry<Long, List<Order>>> entries = new ArrayList<>(triggered.entrySet());
        for (Map.Entry<Long, List<Order>> entry : entries) {
            for (Order stopOrder : entry.getValue()) {
                // 转市价单撮合
                Order marketOrder = toBuilder(stopOrder, Order.OrderType.MARKET, lastPrice);
                List<MatchResult> results = matchMarketOrder(marketOrder);
                allResults.addAll(results);

                log.info("止损单触发: orderId={}, triggerPrice={}, currentPrice={}, trades={}",
                        stopOrder.getOrderId(), entry.getKey(), lastPrice, results.size());
            }
            stopOrders.remove(entry.getKey());
        }

        return new MatchedOrder(null, allResults);
    }

    // ==================== 撤单 ====================

    /**
     * 撤单
     *
     * @param orderId 订单ID
     * @return true=撤单成功
     */
    public boolean cancelOrder(String orderId) {
        return orderBook.cancelOrder(orderId);
    }

    // ==================== 内部撮合逻辑 ====================

    /**
     * 与对手盘逐价位撮合
     *
     * @param taker    主动单
     * @param book     对手盘（已按价格排序）
     * @param isBid    true=对手盘是买盘，false=对手盘是卖盘
     * @param results  输出参数：成交结果列表
     */
    private void matchAgainstBook(Order taker, NavigableMap<Long, Deque<Order>> book,
                                   boolean isBid, List<MatchResult> results) {
        if (book == null || book.isEmpty()) return;

        // 需要保存待移除的价位（遍历时不能直接删除）
        List<Long> emptyLevels = new ArrayList<>();

        Iterator<Map.Entry<Long, Deque<Order>>> it = book.entrySet().iterator();
        while (it.hasNext() && taker.remainingVolume() > 0) {
            Map.Entry<Long, Deque<Order>> level = it.next();
            long levelPrice = level.getKey();
            Deque<Order> queue = level.getValue();

            matchAgainstQueue(taker, levelPrice, queue, emptyLevels, results);
        }

        // 清理空价位
        for (Long price : emptyLevels) {
            if (isBid) {
                orderBook.removeBidsAtPrice(price);
            } else {
                orderBook.removeAsksAtPrice(price);
            }
        }
    }

    /**
     * 与单个价位的订单队列逐笔撮合
     */
    private void matchAgainstQueue(Order taker, long levelPrice, Deque<Order> queue,
                                    List<Long> emptyLevels, List<MatchResult> results) {
        while (!queue.isEmpty() && taker.remainingVolume() > 0) {
            Order maker = queue.peekFirst();
            if (maker == null) break;

            int matchVolume = Math.min(taker.remainingVolume(), maker.remainingVolume());

            MatchResult result = MatchResult.builder()
                    .takerOrderId(taker.getOrderId())
                    .makerOrderId(maker.getOrderId())
                    .price(levelPrice)
                    .volume(matchVolume)
                    .timestamp(System.nanoTime())
                    .symbol(symbol)
                    .build();
            results.add(result);

            // 通过反射更新已成交数量（Order是@Value不可变对象）
            taker.setFilledVolume(taker.getFilledVolume() + matchVolume);
            maker.setFilledVolume(maker.getFilledVolume() + matchVolume);

            // 对手单完全成交后移出队列
            if (maker.remainingVolume() == 0) {
                queue.pollFirst();
            }
        }

        if (queue.isEmpty()) {
            // 标记该价位为空（只标记第一个遇到的空价位）
            if (emptyLevels.isEmpty() || !emptyLevels.contains(levelPrice)) {
                emptyLevels.add(levelPrice);
            }
        }
    }

    /**
     * 计算对手盘可用流动性
     */
    private int calculateAvailableLiquidity(Order taker) {
        int total = 0;
        if (taker.isBuy()) {
            for (Map.Entry<Long, Deque<Order>> entry : orderBook.asks()) {
                for (Order o : entry.getValue()) {
                    total += o.remainingVolume();
                }
            }
        } else {
            for (Map.Entry<Long, Deque<Order>> entry : orderBook.bids()) {
                for (Order o : entry.getValue()) {
                    total += o.remainingVolume();
                }
            }
        }
        return total;
    }

    /**
     * 当前市价（最优买价+最优卖价的中间价）
     */
    public long getCurrentMidPrice() {
        Long bestBid = orderBook.getBestBidPrice();
        Long bestAsk = orderBook.getBestAskPrice();
        if (bestBid == null && bestAsk == null) return 0;
        if (bestBid == null) return bestAsk;
        if (bestAsk == null) return bestBid;
        return (bestBid + bestAsk) / 2;
    }

    // ==================== 工具方法 ====================

    private Order toBuilder(Order original, Order.OrderType newType, long newPrice) {
        return Order.builder()
                .orderId(original.getOrderId())
                .userId(original.getUserId())
                .direction(original.getDirection())
                .type(newType)
                .price(newPrice)
                .volume(original.remainingVolume())
                .filledVolume(0)
                .timestamp(System.nanoTime())
                .build();
    }

    /** 生成唯一成交ID */
    public String generateMatchId() {
        return symbol + "M" + String.format("%012d", matchSeq.incrementAndGet());
    }

    // ==================== 快照支持 ====================

    public OrderBook getOrderBook() { return orderBook; }
    public String getSymbol() { return symbol; }


}
