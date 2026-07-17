package com.futures.matching.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 订单簿（Order Book）。
 * <p>
 * 每个交易品种一个订单簿实例。使用 ConcurrentSkipListMap 实现价格级别管理：
 * <ul>
 *   <li>买单：价格从高到低排序（反向 Comparator）</li>
 *   <li>卖单：价格从低到高排序（自然顺序）</li>
 * </ul>
 * 同一价格级别使用 FIFO 队列（先到先成交）。
 * </p>
 *
 * <p>
 * 设计要点：
 * <ul>
 *   <li><b>单线程写</b>：通过 Disruptor 保证每个 Symbol 的写入在同一线程处理</li>
 *   <li><b>无锁读</b>：ConcurrentSkipListMap 支持并发读</li>
 *   <li><b>内存常驻</b>：所有数据在堆内存中，通过事件日志持久化</li>
 * </ul>
 * </p>
 */
@Slf4j
public class OrderBook {

    /** 合约代码 */
    @Getter
    private final String symbol;

    /** 买盘：价格从高到低排序 */
    private final ConcurrentSkipListMap<Long, PriceLevel> bids;

    /** 卖盘：价格从低到高排序 */
    private final ConcurrentSkipListMap<Long, PriceLevel> asks;

    /** 订单索引：orderId -> MatchingOrder（用于快速撤单） */
    private final Map<String, OrderLocation> orderIndex;

    /** 当前最优买价 */
    @Getter
    private volatile long bestBidEncoded;

    /** 当前最优卖价 */
    @Getter
    private volatile long bestAskEncoded;

    /** 当前最优买价（BigDecimal） */
    @Getter
    private BigDecimal bestBid;

    /** 当前最优卖价（BigDecimal） */
    @Getter
    private BigDecimal bestAsk;

    /** 最近成交价 */
    @Getter
    private volatile long lastPriceEncoded;

    /** 最近成交价（BigDecimal） */
    @Getter
    private BigDecimal lastPrice;

    /** 订单簿版本号（每次变化递增） */
    @Getter
    private long version;

    /**
     * 订单在订单簿中的位置信息。
     */
    private static class OrderLocation {
        final Side side;
        final long priceEncoded;

        OrderLocation(Side side, long priceEncoded) {
            this.side = side;
            this.priceEncoded = priceEncoded;
        }
    }

    public OrderBook(String symbol) {
        this.symbol = symbol;
        // 买单：价格从高到低（反向排序）
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        // 卖单：价格从低到高（自然排序）
        this.asks = new ConcurrentSkipListMap<>(Long::compare);
        this.orderIndex = new HashMap<>(1024);
        this.bestBidEncoded = Long.MIN_VALUE;
        this.bestAskEncoded = Long.MAX_VALUE;
        this.bestBid = BigDecimal.ZERO;
        this.bestAsk = BigDecimal.ZERO;
        this.lastPrice = BigDecimal.ZERO;
        this.version = 0;
    }

    // ==================== 订单操作 ====================

    /**
     * 挂入订单（未立刻成交的部分）。
     *
     * @param order 订单
     */
    public void addOrder(MatchingOrder order) {
        if (order.isFullyFilled() || !order.isActive()) return;

        ConcurrentSkipListMap<Long, PriceLevel> book = bookForSide(order.getSide());
        long encoded = order.getPriceEncoded();

        PriceLevel level = book.computeIfAbsent(encoded, k -> new PriceLevel(order.getPrice(), encoded));
        level.addOrder(order);

        // 记录位置索引
        orderIndex.put(order.getOrderId(), new OrderLocation(order.getSide(), encoded));

        // 更新最优价格
        updateBestPrices();
        version++;
    }

    /**
     * 撤销订单。
     *
     * @param orderId 订单 ID
     * @return 被撤销的订单，null 表示未找到
     */
    public MatchingOrder cancelOrder(String orderId) {
        OrderLocation loc = orderIndex.get(orderId);
        if (loc == null) return null;

        ConcurrentSkipListMap<Long, PriceLevel> book = bookForSide(loc.side);
        PriceLevel level = book.get(loc.priceEncoded);
        if (level == null) return null;

        // 从队列中移除（线性扫描）
        MatchingOrder cancelled = null;
        Iterator<MatchingOrder> it = level.getOrders().iterator();
        while (it.hasNext()) {
            MatchingOrder o = it.next();
            if (o.getOrderId().equals(orderId)) {
                it.remove();
                cancelled = o.toBuilder().active(false).build();
                break;
            }
        }

        // 清理空价格级别
        if (level.isEmpty()) {
            book.remove(loc.priceEncoded);
        }

        orderIndex.remove(orderId);
        updateBestPrices();
        version++;
        return cancelled;
    }

    /**
     * 获取指定方向的最优对手盘（用于市价单）。
     *
     * @param side 我方方向
     * @return 最优对手价格级别的副本，null 表示无对手盘
     */
    public PriceLevel bestOpponentLevel(Side side) {
        ConcurrentSkipListMap<Long, PriceLevel> opponentBook = bookForSide(side.opposite());
        if (opponentBook.isEmpty()) return null;

        Map.Entry<Long, PriceLevel> entry = opponentBook.firstEntry();
        return entry == null ? null : entry.getValue();
    }

    /**
     * 获取可成交的价格级别列表（限价单）。
     * 买单：找所有卖价 ≤ 买单价格的卖盘
     * 卖单：找所有买价 ≥ 卖单价格的买盘
     *
     * @param side        我方方向
     * @param priceEncoded 价格编码
     * @return 可成交的价格级别列表（从最优到最差排序）
     */
    public List<PriceLevel> getMatchableLevels(Side side, long priceEncoded) {
        ConcurrentSkipListMap<Long, PriceLevel> opponentBook = bookForSide(side.opposite());
        List<PriceLevel> result = new ArrayList<>();

        if (side == Side.BUY) {
            // 买单：找所有卖价 ≤ 买单价格
            for (Map.Entry<Long, PriceLevel> entry : opponentBook.entrySet()) {
                if (entry.getKey() <= priceEncoded) {
                    result.add(entry.getValue());
                } else {
                    break;
                }
            }
        } else {
            // 卖单：找所有买价 ≥ 卖单价格
            for (Map.Entry<Long, PriceLevel> entry : opponentBook.entrySet()) {
                if (entry.getKey() >= priceEncoded) {
                    result.add(entry.getValue());
                } else {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 减少价格级别的数量（部分成交后更新）。
     *
     * @param side        方向
     * @param priceEncoded 价格编码
     * @param volume       减少数量
     */
    public void reduceLevelVolume(Side side, long priceEncoded, int volume) {
        PriceLevel level = bookForSide(side).get(priceEncoded);
        if (level != null) {
            // PriceLevel 的 totalVolume 是只读快照，不在这里维护
        }
    }

    /**
     * 移除价格级别中的首个订单（全部成交后）。
     *
     * @param side        方向
     * @param priceEncoded 价格编码
     * @return 被移除的订单
     */
    public MatchingOrder pollHeadFromLevel(Side side, long priceEncoded) {
        ConcurrentSkipListMap<Long, PriceLevel> book = bookForSide(side);
        PriceLevel level = book.get(priceEncoded);
        if (level == null) return null;

        MatchingOrder order = level.pollHead();
        if (order != null) {
            orderIndex.remove(order.getOrderId());
            if (level.isEmpty()) {
                book.remove(priceEncoded);
            }
            updateBestPrices();
            version++;
        }
        return order;
    }

    // ==================== 查询操作 ====================

    /** 获取指定方向的订单簿引用 */
    private ConcurrentSkipListMap<Long, PriceLevel> bookForSide(Side side) {
        return side == Side.BUY ? bids : asks;
    }

    /** 获取买盘深度 */
    public NavigableMap<Long, PriceLevel> getBids() {
        return Collections.unmodifiableNavigableMap(bids);
    }

    /** 获取卖盘深度 */
    public NavigableMap<Long, PriceLevel> getAsks() {
        return Collections.unmodifiableNavigableMap(asks);
    }

    /** 获取订单总数 */
    public int orderCount() {
        return orderIndex.size();
    }

    /** 买盘价格级别数 */
    public int bidLevelCount() {
        return bids.size();
    }

    /** 卖盘价格级别数 */
    public int askLevelCount() {
        return asks.size();
    }

    /** 买盘总数量 */
    public long bidTotalVolume() {
        return bids.values().stream().mapToLong(PriceLevel::getTotalVolume).sum();
    }

    /** 卖盘总数量 */
    public long askTotalVolume() {
        return asks.values().stream().mapToLong(PriceLevel::getTotalVolume).sum();
    }

    /**
     * 是否为买卖均衡状态（价格交叉判定）。
     * 当最优买价 >= 最优卖价时，理论上应立即撮合。
     */
    public boolean hasCrossed() {
        return bestBidEncoded >= bestAskEncoded;
    }

    // ==================== 内部方法 ====================

    /** 更新最优买卖价 */
    private void updateBestPrices() {
        Map.Entry<Long, PriceLevel> bestBidEntry = bids.firstEntry();
        Map.Entry<Long, PriceLevel> bestAskEntry = asks.firstEntry();

        if (bestBidEntry != null) {
            bestBidEncoded = bestBidEntry.getKey();
            bestBid = bestBidEntry.getValue().getPrice();
        } else {
            bestBidEncoded = Long.MIN_VALUE;
            bestBid = BigDecimal.ZERO;
        }

        if (bestAskEntry != null) {
            bestAskEncoded = bestAskEntry.getKey();
            bestAsk = bestAskEntry.getValue().getPrice();
        } else {
            bestAskEncoded = Long.MAX_VALUE;
            bestAsk = BigDecimal.ZERO;
        }
    }

    /** 更新最近成交价 */
    public void updateLastPrice(BigDecimal price) {
        this.lastPrice = price;
        this.lastPriceEncoded = price.multiply(BigDecimal.valueOf(10000)).longValue();
    }

    // ==================== 快照支持 ====================

    /** 导出快照数据 */
    public OrderBookSnapshot toSnapshot() {
        return new OrderBookSnapshot(
                symbol, version,
                new HashMap<>(bids), new HashMap<>(asks),
                bestBid, bestAsk, lastPrice
        );
    }

    /** 从快照恢复 */
    public void restoreFromSnapshot(OrderBookSnapshot snapshot) {
        bids.clear();
        asks.clear();
        orderIndex.clear();
        bids.putAll(snapshot.bids);
        asks.putAll(snapshot.asks);
        // 重建索引
        for (var entry : bids.entrySet()) {
            for (MatchingOrder order : entry.getValue().getOrders()) {
                orderIndex.put(order.getOrderId(), new OrderLocation(Side.BUY, entry.getKey()));
            }
        }
        for (var entry : asks.entrySet()) {
            for (MatchingOrder order : entry.getValue().getOrders()) {
                orderIndex.put(order.getOrderId(), new OrderLocation(Side.SELL, entry.getKey()));
            }
        }
        this.bestBid = snapshot.bestBid;
        this.bestAsk = snapshot.bestAsk;
        this.lastPrice = snapshot.lastPrice;
        this.version = snapshot.version;
        updateBestPrices();
    }

    /**
     * 订单簿快照。
     */
    public record OrderBookSnapshot(
            String symbol,
            long version,
            Map<Long, PriceLevel> bids,
            Map<Long, PriceLevel> asks,
            BigDecimal bestBid,
            BigDecimal bestAsk,
            BigDecimal lastPrice
    ) implements java.io.Serializable {}
}
