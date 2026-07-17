package com.futures.matching.model;

/**
 * 买卖方向枚举。
 */
public enum Side {
    BUY,
    SELL;

    public Side opposite() {
        return this == BUY ? SELL : BUY;
    }
}
