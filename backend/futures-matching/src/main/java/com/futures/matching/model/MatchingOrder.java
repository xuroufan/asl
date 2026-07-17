package com.futures.matching.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;

/**
 * 撮合引擎内部使用的不可变订单模型。
 * <p>
 * 创建后不可修改，保证了多线程可见性。价格以 long 型整数存储（price * 10000）
 * 避免 BigDecimal 的创建开销和比较性能问题。</p>
 */
@Value
@Builder(toBuilder = true)
public class MatchingOrder implements Comparable<MatchingOrder> {

    /** 订单 ID */
    String orderId;

    /** 用户 ID */
    long userId;

    /** 合约代码 */
    String symbol;

    /** 买卖方向 */
    Side side;

    /** 订单类型 */
    OrderType orderType;

    /** 价格（原始 BigDecimal） */
    BigDecimal price;

    /** 价格编码（price * 10000，用于高性能比较） */
    long priceEncoded;

    /** 数量 */
    int volume;

    /** 已成交数量 */
    int filledVolume;

    /** 止损/止盈触发价 */
    BigDecimal triggerPrice;

    /** 止损/止盈触发价编码 */
    long triggerPriceEncoded;

    /** 创建时间戳（纳秒） */
    long createdAtNanos;

    /** 是否活跃（未完全成交且未取消） */
    boolean active;

    /**
     * 按价格+时间优先排序。
     * 买单：价格高优先，价格相同时间早优先。
     * 卖单：价格低优先，价格相同时间早优先。
     */
    @Override
    public int compareTo(MatchingOrder o) {
        int cmp;
        if (side == Side.BUY) {
            cmp = Long.compare(o.priceEncoded, this.priceEncoded); // 买价高优先
        } else {
            cmp = Long.compare(this.priceEncoded, o.priceEncoded); // 卖价低优先
        }
        if (cmp != 0) return cmp;
        return Long.compare(this.createdAtNanos, o.createdAtNanos); // 时间优先
    }

    /** 工厂方法：从 PlaceOrderEvent 创建撮合订单 */
    public static MatchingOrder from(long userId, String symbol, Side side,
                                      OrderType orderType, BigDecimal price, int volume,
                                      BigDecimal triggerPrice) {
        long now = System.nanoTime();
        return MatchingOrder.builder()
                .orderId("MATCH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .orderType(orderType)
                .price(price)
                .priceEncoded(price == null ? 0 : price.multiply(BigDecimal.valueOf(10000)).longValue())
                .volume(volume)
                .filledVolume(0)
                .triggerPrice(triggerPrice)
                .triggerPriceEncoded(triggerPrice == null ? 0 : triggerPrice.multiply(BigDecimal.valueOf(10000)).longValue())
                .createdAtNanos(now)
                .active(true)
                .build();
    }

    /** 剩余未成交数量 */
    public int remainingVolume() {
        return volume - filledVolume;
    }

    /** 是否全部成交 */
    public boolean isFullyFilled() {
        return filledVolume >= volume;
    }
}
