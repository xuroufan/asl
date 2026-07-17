package com.futures.order.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 订单类型枚举
 * LIMIT=0(限价), MARKET=1(市价), STOP=2(止损)
 */
public enum OrderType implements IEnum<Integer> {
    LIMIT(0),
    MARKET(1),
    STOP(2);

    private final int value;

    OrderType(int value) { this.value = value; }

    @Override
    public Integer getValue() { return value; }

    public static OrderType fromString(String s) {
        if ("LIMIT".equalsIgnoreCase(s)) return LIMIT;
        if ("MARKET".equalsIgnoreCase(s)) return MARKET;
        if ("STOP".equalsIgnoreCase(s)) return STOP;
        throw new IllegalArgumentException("未知订单类型: " + s);
    }
}
