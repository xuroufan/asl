package com.futures.order.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 订单状态枚举
 * PENDING=0(已报), PARTIAL=1(部成), FILLED=2(全成),
 * CANCELLED=3(已撤), REJECTED=4(废单)
 */
public enum OrderStatus implements IEnum<Integer> {
    PENDING(0),
    PARTIAL(1),
    FILLED(2),
    CANCELLED(3),
    REJECTED(4);

    private final int value;

    OrderStatus(int value) { this.value = value; }

    @Override
    public Integer getValue() { return value; }

    public boolean isActive() {
        return this == PENDING || this == PARTIAL;
    }

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED || this == REJECTED;
    }
}
