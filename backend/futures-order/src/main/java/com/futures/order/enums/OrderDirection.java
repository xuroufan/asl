package com.futures.order.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 订单方向枚举
 * BUY=0(买入/开多), SELL=1(卖出/开空)
 */
public enum OrderDirection implements IEnum<Integer> {
    BUY(0),
    SELL(1);

    private final int value;

    OrderDirection(int value) { this.value = value; }

    @Override
    public Integer getValue() { return value; }

    public static OrderDirection fromString(String s) {
        if ("BUY".equalsIgnoreCase(s)) return BUY;
        if ("SELL".equalsIgnoreCase(s)) return SELL;
        throw new IllegalArgumentException("未知方向: " + s);
    }
}
