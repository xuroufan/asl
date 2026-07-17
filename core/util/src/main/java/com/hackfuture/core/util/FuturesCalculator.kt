package com.hackfuture.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 期货交易专用计算工具
 *
 * 集成自 Shinny Futures MathUtils + 中国期货市场规则：
 * - 保证金计算
 * - 盈亏计算
 * - 风险度
 * = 最小变动价位
 * - 合约乘数
 */
object FuturesCalculator {

    // ============ 精度计算 ============

    fun add(v1: BigDecimal, v2: BigDecimal): BigDecimal = v1.add(v2)

    fun subtract(v1: BigDecimal, v2: BigDecimal): BigDecimal = v1.subtract(v2)

    fun multiply(v1: BigDecimal, v2: BigDecimal): BigDecimal = v1.multiply(v2)

    fun divide(v1: BigDecimal, v2: BigDecimal, scale: Int = 10): BigDecimal =
        try { v1.divide(v2, scale, RoundingMode.HALF_EVEN) }
        catch (_: ArithmeticException) { v1 }

    fun round(v: BigDecimal, scale: Int): BigDecimal =
        v.setScale(scale, RoundingMode.HALF_EVEN)

    /**
     * 去除小数点后多余的0
     */
    fun stripTrailingZeros(v: String): String =
        try { BigDecimal(v).stripTrailingZeros().toPlainString() }
        catch (_: Exception) { v }

    // ============ 盈亏计算 ============

    /**
     * 计算多头持仓浮动盈亏
     *
     * @param currentPrice 当前价（最新价或结算价）
     * @param openPrice 开仓均价
     * @param volume 手数
     * @param multiplier 合约乘数
     */
    fun calcLongProfit(
        currentPrice: BigDecimal,
        openPrice: BigDecimal,
        volume: Int,
        multiplier: BigDecimal
    ): BigDecimal =
        (currentPrice - openPrice) * BigDecimal.valueOf(volume.toLong()) * multiplier

    /**
     * 计算空头持仓浮动盈亏
     */
    fun calcShortProfit(
        currentPrice: BigDecimal,
        openPrice: BigDecimal,
        volume: Int,
        multiplier: BigDecimal
    ): BigDecimal =
        (openPrice - currentPrice) * BigDecimal.valueOf(volume.toLong()) * multiplier

    // ============ 保证金 ============

    /**
     * 计算持仓保证金
     *
     * @param price 当前价格（开仓价或结算价）
     * @param volume 手数
     * @param multiplier 合约乘数
     * @param marginRate 保证金率 (如 0.12 = 12%)
     */
    fun calcMargin(
        price: BigDecimal,
        volume: Int,
        multiplier: BigDecimal,
        marginRate: BigDecimal
    ): BigDecimal =
        price * BigDecimal.valueOf(volume.toLong()) * multiplier * marginRate

    // ============ 风险度 ============

    /**
     * 计算风险度
     *
     * 风险度 = 占用保证金 / 动态权益 × 100%
     *
     * > 100%: 超风险，可能被强平
     * > 80%: 需要注意追加保证金
     * < 30%: 安全
     */
    fun calcRiskRatio(
        usedMargin: BigDecimal,
        totalEquity: BigDecimal
    ): BigDecimal {
        if (totalEquity <= BigDecimal.ZERO) return BigDecimal.valueOf(100)
        return (usedMargin / totalEquity * BigDecimal(100))
            .setScale(2, RoundingMode.HALF_EVEN)
    }

    /**
     * 计算可开仓手数
     */
    fun calcOpenableVolume(
        available: BigDecimal,
        price: BigDecimal,
        multiplier: BigDecimal,
        marginRate: BigDecimal
    ): Int {
        val marginPerLot = price * multiplier * marginRate
        if (marginPerLot <= BigDecimal.ZERO) return 0
        return (available / marginPerLot).toInt()
    }

    // ============ 手续费 ============

    /**
     * 按固定值计算手续费
     */
    fun calcCommissionFixed(volume: Int, fixedRate: BigDecimal): BigDecimal =
        BigDecimal.valueOf(volume.toLong()) * fixedRate

    /**
     * 按成交额比例计算手续费
     */
    fun calcCommissionRatio(turnover: BigDecimal, ratioRate: BigDecimal): BigDecimal =
        turnover * ratioRate

    // ============ 涨跌幅 ============

    /**
     * 计算涨跌幅
     */
    fun calcChange(current: BigDecimal, preClose: BigDecimal): BigDecimal =
        if (preClose > BigDecimal.ZERO)
            (current - preClose).setScale(2, RoundingMode.HALF_EVEN)
        else BigDecimal.ZERO

    /**
     * 计算涨跌幅百分比
     */
    fun calcChangePercent(current: BigDecimal, preClose: BigDecimal): BigDecimal {
        if (preClose <= BigDecimal.ZERO) return BigDecimal.ZERO
        return ((current - preClose) / preClose * BigDecimal(100))
            .setScale(2, RoundingMode.HALF_EVEN)
    }

    // ============ 涨跌停 ============

    /**
     * 计算涨停价
     */
    fun calcUpperLimit(preSettlement: BigDecimal, limitPercent: BigDecimal): BigDecimal =
        (preSettlement * (BigDecimal.ONE + limitPercent))
            .setScale(2, RoundingMode.HALF_EVEN)

    /**
     * 计算跌停价
     */
    fun calcLowerLimit(preSettlement: BigDecimal, limitPercent: BigDecimal): BigDecimal =
        (preSettlement * (BigDecimal.ONE - limitPercent))
            .setScale(2, RoundingMode.HALF_EVEN)
}
