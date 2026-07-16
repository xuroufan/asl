package com.hackfuture.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 国际化格式化工具 — 根据当前 Locale 格式化价格、日期、成交量等。
 *
 * 使用方式：
 *   FormatUtils.formatPrice(12345.67, locale)  →  "12,345.67" (en) / "12345.67" (zh)
 *   FormatUtils.formatDate(timestamp, locale)   →  "Jul 15, 2026" (en) / "2026年7月15日" (zh)
 */
object FormatUtils {

    // ==================== 价格格式化 ====================

    /**
     * 格式化价格为两位小数，跟随 Locale 的千分位分隔符习惯。
     *
     * - en: 20000.50 → "20,000.50"
     * - zh: 20000.50 → "20000.50"
     * - zh-rTW: 20000.50 → "20,000.50"
     */
    fun formatPrice(value: Double, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return format.format(value)
    }

    /**
     * 格式化数量（最多 4 位小数）。
     */
    fun formatQuantity(value: Double, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 4
        format.isGroupingUsed = false
        return format.format(value)
    }

    /**
     * 格式化涨跌幅百分比（带符号）。
     *
     * - en: +1.85% / -0.50%
     * - zh: +1.85% / -0.50%
     */
    fun formatPercent(value: Double, locale: Locale = Locale.getDefault()): String {
        val prefix = if (value >= 0) "+" else ""
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return "${prefix}${format.format(value)}%"
    }

    // ==================== 日期/时间格式化 ====================

    /**
     * 格式化日期（年月日），跟随 Locale。
     *
     * - en: Jul 15, 2026
     * - zh: 2026年7月15日
     * - zh-rTW: 2026年7月15日
     */
    fun formatDate(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = when {
            locale.language == "zh" -> "yyyy年M月d日"
            else -> "MMM d, yyyy"
        }
        val sdf = SimpleDateFormat(pattern, locale)
        return sdf.format(Date(timestampMs))
    }

    /**
     * 格式化为完整日期时间。
     *
     * - en: Jul 15, 2026 14:30
     * - zh: 2026年7月15日 14:30
     */
    fun formatDateTime(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val datePattern = when {
            locale.language == "zh" -> "yyyy年M月d日 HH:mm"
            else -> "MMM d, yyyy HH:mm"
        }
        val sdf = SimpleDateFormat(datePattern, locale)
        return sdf.format(Date(timestampMs))
    }

    /**
     * 格式化为仅时间（HH:mm:ss）。
     */
    fun formatTime(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("HH:mm:ss", locale)
        return sdf.format(Date(timestampMs))
    }

    // ==================== 紧凑格式化 ====================

    /**
     * 格式化为中文大额缩写（万/亿）。
     * 英文环境下保持原值加逗号。
     */
    fun formatCompact(value: Double, locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "zh") {
            when {
                value >= 100_000_000 -> "${formatPrice(value / 100_000_000.0, locale)}亿"
                value >= 10_000 -> "${formatPrice(value / 10_000.0, locale)}万"
                else -> formatPrice(value, locale)
            }
        } else {
            val suffixes = arrayOf("", "K", "M", "B", "T")
            var v = value
            var idx = 0
            while (v >= 1000 && idx < suffixes.size - 1) {
                v /= 1000
                idx++
            }
            if (idx == 0) formatPrice(value, locale)
            else "${formatPrice(v, locale)}${suffixes[idx]}"
        }
    }

    /**
     * 格式化成交量（长整型）。
     */
    fun formatVolume(volume: Long, locale: Locale = Locale.getDefault()): String {
        return formatCompact(volume.toDouble(), locale)
    }
}
