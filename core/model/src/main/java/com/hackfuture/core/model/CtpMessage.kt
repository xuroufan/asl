package com.hackfuture.core.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * CTP 协议消息类型
 *
 * 对应 Shinny Futures DataManager 中解析的 aid 字段
 */
@Serializable
enum class CtpMessageType(val aid: String) {
    /** 实时数据推送 (行情、订单、持仓、成交、账户) */
    RTN_DATA("rtn_data"),

    /** 登录响应 */
    RSP_LOGIN("rsp_login"),

    /** 登出响应 */
    RSP_LOGOUT("rsp_logout"),

    /** 下单响应 */
    RSP_ORDER("rsp_order"),

    /** 撤单响应 */
    RSP_CANCEL("rsp_cancel"),

    /** 经纪人列表 */
    RTN_BROKERS("rtn_brokers"),

    /** 合约信息 */
    RTN_INSTRUMENTS("rtn_instruments"),

    /** 错误消息 */
    RTN_ERROR("rtn_error"),

    /** 未知类型 */
    UNKNOWN("");

    companion object {
        fun fromAid(aid: String?): CtpMessageType =
            entries.firstOrNull { it.aid == aid } ?: UNKNOWN
    }
}

/**
 * CTP WebSocket 通用消息结构
 *
 * 所有 CTP WebSocket 消息都包含 aid 字段标识消息类型。
 * 消息体可以是行情、交易信息或系统消息。
 */
@Serializable
data class CtpMessage(
    @Contextual
    val aid: String,
    // val data: Map<String, Any>? = null,  // removed - requires contextual serializer
    val msg: String? = null,
    val error: String? = null,
) {
    val type: CtpMessageType get() = CtpMessageType.fromAid(aid)
    val isSuccess: Boolean get() = error.isNullOrEmpty()
}

/**
 * CTP Broker 信息
 */
@Serializable
data class CtpBrokerInfo(
    val brokerId: String,
    val brokerName: String,
)

/**
 * CTP 合约信息
 */
@Serializable
data class CtpInstrumentInfo(
    val instrumentId: String,
    val instrumentName: String,
    val exchangeId: String,
    val productClass: String,       // 1=期货, 2=期权
    val deliveryYear: Int,
    val deliveryMonth: Int,
    val volumeMultiple: Int,        // 合约乘数
    val priceTick: Double,          // 最小变动价位
    val longMarginRatio: Double,    // 多头保证金率
    val shortMarginRatio: Double,   // 空头保证金率
    val upperLimitPrice: Double,    // 涨停价
    val lowerLimitPrice: Double,    // 跌停价
    val preSettlementPrice: Double, // 昨结算
    val strikePrice: Double? = null,// 行权价 (期权)
)
