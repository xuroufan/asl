package com.futures.matching.model;

/**
 * 撮合引擎支持的订单类型。
 */
public enum OrderType {
    LIMIT,          // 限价单
    MARKET,         // 市价单
    STOP,           // 止损单（触及转市价）
    TAKE_PROFIT,    // 止盈单（触及转限价）
    FOK,            // Fill or Kill
    IOC             // Immediate or Cancel
}
