package com.hackfuture.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 数字格式化工具（交易金额、数量显示用）
 */
object NumberUtil {

    private val priceFormat = DecimalFormat("#,##0.00")
    private val quantityFormat = DecimalFormat("#,##0.########")
    private val percentFormat = DecimalFormat("+#,##0.00;-#,##0.00")
    private val compactFormat = DecimalFormat("#,##0.00")

    /**
     * 格式化价格为两位小数
     */
    fun formatPrice(value: Double): String = priceFormat.format(value)

    /**
     * 格式化数量（最多8位小数，去除多余末尾0）
     */
    fun formatQuantity(value: Double): String = quantityFormat.format(value)

    /**
     * 格式化涨跌幅百分比（带符号）
     */
    fun formatPercent(value: Double): String = "${percentFormat.format(value)}%"

    /**
     * 格式化金额（大额自动加逗号）
     */
    fun formatAmount(value: Double): String = compactFormat.format(value)

    /**
     * 四舍五入到指定位数
     */
    fun round(value: Double, scale: Int = 2): Double =
        BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toDouble()

    /**
     * 截断到指定位数（不四舍五入）
     */
    fun truncate(value: Double, scale: Int = 2): Double =
        BigDecimal.valueOf(value).setScale(scale, RoundingMode.DOWN).toDouble()

    /**
     * 格式化为中文大额缩写（万/亿）
     */
    fun formatCompactChinese(value: Double): String {
        return when {
            value >= 1_0000_0000 -> "${formatPrice(value / 1_0000_0000)}亿"
            value >= 1_0000 -> "${formatPrice(value / 1_0000)}万"
            else -> formatPrice(value)
        }
    }
}
