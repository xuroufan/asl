package com.futures.common.message;

/**
 * RocketMQ Topic 常量定义。
 * <p>
 * 所有 Topic 命名规则：futures-{业务领域}-{事件名称}
 * 队列数参考：核心交易链路8队列，行情数据16队列，通知类2队列
 */
public final class RocketMQTopic {

    private RocketMQTopic() {}

    /** 订单创建消息 - 订单服务 → 撮合引擎（事务消息，8队列） */
    public static final String ORDER_CREATED = "futures-order-created";

    /** 订单成交消息 - 撮合引擎 → 订单/账户/风控（8队列） */
    public static final String ORDER_MATCHED = "futures-order-matched";

    /** 订单取消消息 - 订单服务/撮合引擎 → 资金服务（4队列） */
    public static final String ORDER_CANCELLED = "futures-order-cancelled";

    /** 持仓变动消息 - 撮合引擎 → 账户/风控（4队列） */
    public static final String POSITION_CHANGED = "futures-position-changed";

    /** 风控预警消息 - 风控引擎 → 通知服务（2队列，高优先） */
    public static final String RISK_ALERT = "futures-risk-alert";

    /** 行情Tick消息 - 行情服务 → 风控/终端（16队列，高吞吐） */
    public static final String MARKET_TICK = "futures-market-tick";

    /** 结算完成消息 - 清结算 → 账户/通知（4队列） */
    public static final String SETTLEMENT_DONE = "futures-settlement-done";
}
