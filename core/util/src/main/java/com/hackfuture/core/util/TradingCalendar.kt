package com.hackfuture.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 国内期货交易日历 — 区分日盘和夜盘交易时段
 *
 * 参考 Shinny Futures TimeUtils + 国内四大期货交易所规则：
 * - 上期所 (SHFE)
 * - 大商所 (DCE)
 * - 郑商所 (CZCE)
 * - 中金所 (CFFEX)
 * - 能源中心 (INE)
 * - 广期所 (GFEX)
 */
object TradingCalendar {

    // ============ 交易时段定义 ============

    /** 日盘连续竞价 */
    val DAY_SESSION_1 = LocalTime.of(9, 0) to LocalTime.of(10, 15)   // 09:00-10:15
    val DAY_SESSION_2 = LocalTime.of(10, 30) to LocalTime.of(11, 30)  // 10:30-11:30
    val DAY_SESSION_3 = LocalTime.of(13, 30) to LocalTime.of(15, 0)   // 13:30-15:00

    /** 夜盘 */
    val NIGHT_SESSION_START = LocalTime.of(21, 0)                      // 21:00 开始
    val NIGHT_SESSION_END = LocalTime.of(23, 0)                        // 23:00 结束 (部分品种至 23:00 或 01:00 或 02:30)

    // 部分品种夜盘截止时间
    val NIGHT_SESSION_EARLY = LocalTime.of(23, 0)   // 大多数
    val NIGHT_SESSION_LATE = LocalTime.of(1, 0)     // 贵金属
    val NIGHT_SESSION_2_30 = LocalTime.of(2, 30)    // 原油

    /**
     * 判断当前时间是否在日盘交易时段内
     */
    fun isInDaySession(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val time = now.toLocalTime()
        val dow = now.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false

        return (time in DAY_SESSION_1.first..DAY_SESSION_1.second) ||
               (time in DAY_SESSION_2.first..DAY_SESSION_2.second) ||
               (time in DAY_SESSION_3.first..DAY_SESSION_3.second)
    }

    /**
     * 判断当前时间是否在夜盘交易时段内
     * (夜盘属于下一交易日，周一至周五 21:00 至次日 02:30)
     */
    fun isInNightSession(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val time = now.toLocalTime()
        val dow = now.dayOfWeek

        // 周五夜盘可交易
        if (dow == DayOfWeek.SATURDAY) return false
        if (dow == DayOfWeek.SUNDAY) return false

        return time >= NIGHT_SESSION_START || time < NIGHT_SESSION_EARLY
    }

    /**
     * 判断当前是否在交易时间内
     */
    fun isTradingTime(now: LocalDateTime = LocalDateTime.now()): Boolean =
        isInDaySession(now) || isInNightSession(now)

    /**
     * 获取当前所属的交易日
     *
     * 夜盘（21:00 后）属于下一交易日。
     * 例如：周五晚 21:00-23:00 的交易属于下周一。
     */
    fun getCurrentTradingDay(now: LocalDateTime = LocalDateTime.now()): LocalDate {
        val time = now.toLocalTime()
        // 夜盘 21:00 后算下一交易日
        if (time >= NIGHT_SESSION_START) {
            return now.toLocalDate().plusDays(1).let {
                // 跳过周末
                if (it.dayOfWeek == DayOfWeek.SATURDAY) it.plusDays(2)
                else if (it.dayOfWeek == DayOfWeek.SUNDAY) it.plusDays(1)
                else it
            }
        }
        // 凌晨 00:00-02:30 仍在上个交易日夜盘，交易日不变
        return now.toLocalDate()
    }

    /**
     * 格式化 CTP 时间戳（微秒）为可读时间
     */
    fun formatCtpTimestamp(micros: Long): String {
        if (micros <= 0) return "--"
        return try {
            val millis = micros / 1000
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis),
                java.time.ZoneId.of("Asia/Shanghai")
            ).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        } catch (_: Exception) {
            "--"
        }
    }

    /**
     * 判断是否为夜盘时间（用于排序逻辑，Shinny 兼容）
     */
    fun isBetween21And24(micros: Long): Boolean {
        if (micros <= 0) return false
        return try {
            val millis = micros / 1000
            val time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis),
                java.time.ZoneId.of("Asia/Shanghai")
            ).toLocalTime()
            time >= NIGHT_SESSION_START
        } catch (_: Exception) {
            false
        }
    }
}
