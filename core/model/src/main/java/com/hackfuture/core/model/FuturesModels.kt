package com.hackfuture.core.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * 期货交易方向
 * 对应 CTP 协议 TThostFtdcDirectionType
 */
enum class TradeDirection(val code: String, val label: String) {
    BUY("0", "买"),
    SELL("1", "卖");

    companion object {
        fun fromCode(code: String): TradeDirection =
            entries.firstOrNull { it.code == code } ?: BUY
    }
}

/**
 * 开平标志
 * 对应 CTP 协议 TThostFtdcOffsetFlagType
 */
enum class OffsetFlag(val code: String, val label: String) {
    OPEN("0", "开仓"),
    CLOSE("1", "平仓"),
    CLOSE_TODAY("3", "平今"),
    CLOSE_HISTORY("4", "平昨");

    companion object {
        fun fromCode(code: String): OffsetFlag =
            entries.firstOrNull { it.code == code } ?: OPEN
    }
}

/**
 * 订单价格类型
 * 对应 CTP TThostFtdcOrderPriceTypeType
 */
enum class PriceType(val code: String, val label: String) {
    LIMIT("1", "限价"),
    MARKET("2", "市价"),
    FAK("3", "FAK"),
    FOK("4", "FOK");

    companion object {
        fun fromCode(code: String): PriceType =
            entries.firstOrNull { it.code == code } ?: LIMIT
    }
}

/**
 * 订单状态
 * 对应 CTP TThostFtdcOrderStatusType
 */
enum class CtpOrderStatus(val code: String, val label: String) {
    ALL_TRADED("0", "全部成交"),
    PART_TRADED("1", "部分成交"),
    NOT_TRADED("2", "未成交"),
    CANCELLED("3", "已撤单"),
    REJECTED("4", "已拒绝"),
    PART_TRADED_CANCELLED("5", "部成部撤"),
    UNKNOWN("a", "未知");

    companion object {
        fun fromCode(code: String): CtpOrderStatus =
            entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/**
 * 增强型订单模型 — 完全对齐 CTP 协议字段
 *
 * Shinny Futures OrderEntity 的 Kotlin 版本，
 * 融合了 ASL 项目原有的 Order 模型简化结构。
 */
data class FuturesOrder(
    val orderId: String = "",
    val exchangeId: String = "",
    val instrumentId: String = "",
    val userId: String = "",

    // CTP 交易核心字段
    val direction: TradeDirection = TradeDirection.BUY,
    val offset: OffsetFlag = OffsetFlag.OPEN,
    val priceType: PriceType = PriceType.LIMIT,
    val limitPrice: BigDecimal = BigDecimal.ZERO,
    val stopPrice: BigDecimal? = null,

    // 数量信息
    val volumeOrign: Int = 0,
    val volumeLeft: Int = 0,
    val volumeFilled: Int = 0,

    // 状态
    val status: CtpOrderStatus = CtpOrderStatus.NOT_TRADED,
    val lastMsg: String = "",

    // CTP 扩展信息
    val sessionId: String = "",
    val frontId: String = "",
    val exchangeOrderId: String = "",
    val orderType: String = "",          // 0=普通, 1=条件, 2=止损
    val tradeType: String = "",          // 0=投机, 1=套保, 2=套利
    val hedgeFlag: String = "",
    val timeCondition: String = "GFD",   // GFD=当日有效, IOC=立即撤销
    val volumeCondition: String = "ANY",
    val minVolume: Int = 1,
    val forceClose: String = "0",

    // 时间
    val insertDateTime: Long = 0L,       // CTP 时间戳 (微秒)
    val updateTime: Long = 0L,

    // 客户端辅助
    val isRtn: Boolean = false,
)

/**
 * 增强型持仓模型 — 多空双向分开统计
 *
 * Shinny Futures PositionEntity 的 Kotlin 版本。
 * CTP 协议中持仓按品种（instrument_id）聚合，
 * 多/空各项分开记录。
 */
data class FuturesPosition(
    val instrumentId: String = "",
    val exchangeId: String = "",

    // ========== 多头 ==========
    val volumeLongToday: Int = 0,        // 今仓手数
    val volumeLongHistory: Int = 0,      // 昨仓手数
    val volumeLongFrozen: Int = 0,       // 冻结手数
    val volumeLongFrozenToday: Int = 0,  // 今仓冻结
    val openPriceLong: BigDecimal = BigDecimal.ZERO,
    val openCostLong: BigDecimal = BigDecimal.ZERO,
    val positionCostLong: BigDecimal = BigDecimal.ZERO,
    val floatProfitLong: BigDecimal = BigDecimal.ZERO,
    val positionProfitLong: BigDecimal = BigDecimal.ZERO,
    val marginLong: BigDecimal = BigDecimal.ZERO,

    // ========== 空头 ==========
    val volumeShortToday: Int = 0,
    val volumeShortHistory: Int = 0,
    val volumeShortFrozen: Int = 0,
    val volumeShortFrozenToday: Int = 0,
    val openPriceShort: BigDecimal = BigDecimal.ZERO,
    val openCostShort: BigDecimal = BigDecimal.ZERO,
    val positionCostShort: BigDecimal = BigDecimal.ZERO,
    val floatProfitShort: BigDecimal = BigDecimal.ZERO,
    val positionProfitShort: BigDecimal = BigDecimal.ZERO,
    val marginShort: BigDecimal = BigDecimal.ZERO,

    // ========== 委托挂单量 ==========
    val orderVolumeBuyOpen: Int = 0,
    val orderVolumeBuyClose: Int = 0,
    val orderVolumeSellOpen: Int = 0,
    val orderVolumeSellClose: Int = 0,
) {
    /** 总多头手数 */
    val totalLongVolume: Int get() = volumeLongToday + volumeLongHistory
    /** 总空头手数 */
    val totalShortVolume: Int get() = volumeLongToday + volumeLongHistory
    /** 总浮盈 */
    val totalFloatProfit: BigDecimal get() = floatProfitLong + floatProfitShort
    /** 总持仓保证金 */
    val totalMargin: BigDecimal get() = marginLong + marginShort
}

/**
 * 增强型成交记录模型
 */
data class FuturesTrade(
    val tradeId: String = "",
    val orderId: String = "",
    val exchangeId: String = "",
    val instrumentId: String = "",
    val exchangeTradeId: String = "",

    val direction: TradeDirection = TradeDirection.BUY,
    val offset: OffsetFlag = OffsetFlag.OPEN,
    val volume: Int = 0,
    val price: BigDecimal = BigDecimal.ZERO,
    val tradeDateTime: Long = 0L,
)

/**
 * 完整行情报价 — 对齐国内期货五档行情
 */
data class MarketQuote(
    val instrumentId: String = "",
    val instrumentName: String = "",

    // 实时数据
    val lastPrice: BigDecimal = BigDecimal.ZERO,
    val change: BigDecimal = BigDecimal.ZERO,
    val changePercent: BigDecimal = BigDecimal.ZERO,

    // 五档行情
    val askPrice1: BigDecimal = BigDecimal.ZERO,
    val askVolume1: Int = 0,
    val bidPrice1: BigDecimal = BigDecimal.ZERO,
    val bidVolume1: Int = 0,

    // 扩展档位 (CTP 支持五档)
    val askPrice2: BigDecimal? = null,
    val askVolume2: Int? = null,
    val bidPrice2: BigDecimal? = null,
    val bidVolume2: Int? = null,
    val askPrice3: BigDecimal? = null,
    val askVolume3: Int? = null,
    val bidPrice3: BigDecimal? = null,
    val bidVolume3: Int? = null,

    // 日间统计
    val open: BigDecimal = BigDecimal.ZERO,
    val high: BigDecimal = BigDecimal.ZERO,
    val low: BigDecimal = BigDecimal.ZERO,
    val close: BigDecimal = BigDecimal.ZERO,

    // 成交量与持仓量
    val volume: Long = 0,
    val amount: BigDecimal = BigDecimal.ZERO,          // 成交额
    val openInterest: Long = 0,                       // 当前持仓量
    val preOpenInterest: Long = 0,                    // 昨持仓

    // 结算价
    val preSettlement: BigDecimal = BigDecimal.ZERO,  // 昨结算
    val settlement: BigDecimal = BigDecimal.ZERO,     // 今结算
    val preClose: BigDecimal = BigDecimal.ZERO,

    // 涨跌停
    val upperLimit: BigDecimal = BigDecimal.ZERO,     // 涨停价
    val lowerLimit: BigDecimal = BigDecimal.ZERO,     // 跌停价

    val average: BigDecimal = BigDecimal.ZERO,        // 均价
    val datetime: Long = 0L,                          // 行情时间
    val status: String = "",                          // 合约状态
)

/**
 * K线柱模型 — 带开/末持仓量
 */
data class KlineBar(
    val datetime: Long = 0L,
    val open: BigDecimal = BigDecimal.ZERO,
    val high: BigDecimal = BigDecimal.ZERO,
    val low: BigDecimal = BigDecimal.ZERO,
    val close: BigDecimal = BigDecimal.ZERO,
    val volume: Long = 0,
    val openOi: Long = 0,      // 开盘持仓量
    val closeOi: Long = 0,     // 收盘持仓量
)

/**
 * Tick 数据 — 逐笔行情
 */
data class TickData(
    val datetime: Long = 0L,
    val tradingDay: String = "",
    val lastPrice: BigDecimal = BigDecimal.ZERO,
    val volume: Long = 0,
    val openInterest: Long = 0,
    val bidPrice1: BigDecimal = BigDecimal.ZERO,
    val askPrice1: BigDecimal = BigDecimal.ZERO,
    val bidVolume1: Int = 0,
    val askVolume1: Int = 0,
    val highest: BigDecimal = BigDecimal.ZERO,
    val lowest: BigDecimal = BigDecimal.ZERO,
)

/**
 * 账户信息 — CTP 账户资金字段
 */
data class AccountInfo(
    val accountId: String = "",
    val currency: String = "CNY",

    val balance: BigDecimal = BigDecimal.ZERO,              // 动态权益
    val available: BigDecimal = BigDecimal.ZERO,            // 可用资金
    val preBalance: BigDecimal = BigDecimal.ZERO,           // 昨权益
    val staticBalance: BigDecimal = BigDecimal.ZERO,        // 静态权益

    val deposit: BigDecimal = BigDecimal.ZERO,              // 入金
    val withdraw: BigDecimal = BigDecimal.ZERO,             // 出金
    val commission: BigDecimal = BigDecimal.ZERO,           // 手续费
    val premium: BigDecimal = BigDecimal.ZERO,              // 权利金 (期权)

    val positionProfit: BigDecimal = BigDecimal.ZERO,       // 平仓盈亏
    val floatProfit: BigDecimal = BigDecimal.ZERO,          // 持仓盈亏
    val closeProfit: BigDecimal = BigDecimal.ZERO,          // 平仓盈亏明细

    val margin: BigDecimal = BigDecimal.ZERO,               // 占用保证金
    val frozenMargin: BigDecimal = BigDecimal.ZERO,         // 冻结保证金
    val frozenCommission: BigDecimal = BigDecimal.ZERO,     // 冻结手续费
    val frozenPremium: BigDecimal = BigDecimal.ZERO,        // 冻结权利金

    val riskRatio: BigDecimal = BigDecimal.ZERO,            // 风险度
) {
    /** 总权益 = 静态权益 + 平仓盈亏 + 持仓盈亏 */
    val totalEquity: BigDecimal get() = staticBalance + closeProfit + floatProfit
}
