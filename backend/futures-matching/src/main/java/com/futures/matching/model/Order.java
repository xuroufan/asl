package com.futures.matching.model;

import lombok.Builder;
import lombok.Data;

/**
 * 订单实体 — 不可变对象
 */
@Data
@Builder
public class Order {
    String orderId;
    String userId;
    String symbol;
    Direction direction;   // BUY / SELL
    OrderType type;        // LIMIT / MARKET / STOP / TAKE_PROFIT / FOK / IOC
    long price;            // 单位：最小变动价位整数
    int volume;            // 手数
    int filledVolume;      // 已成交量
    long timestamp;        // 创建时间戳（纳秒）

    public boolean isBuy() { return direction == Direction.BUY; }
    public boolean isSell() { return direction == Direction.SELL; }
    public boolean isLimit() { return type == OrderType.LIMIT; }
    public boolean isMarket() { return type == OrderType.MARKET; }

    public int remainingVolume() { return volume - filledVolume; }

    public enum Direction { BUY, SELL }
    public enum OrderType { LIMIT, MARKET, STOP, TAKE_PROFIT, FOK, IOC }
}
