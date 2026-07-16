package com.hackfuture.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 日期时间工具类（基于 java.time，minSdk 26+ 可用）
 */
object DateTimeUtil {

    private val defaultZone = ZoneId.of("Asia/Shanghai")

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val shortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    /**
     * 将时间戳转为 LocalDateTime（默认 Asia/Shanghai 时区）
     */
    fun toLocalDateTime(timestampMs: Long, zone: ZoneId = defaultZone): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), zone)

    /**
     * 将时间戳格式化为 "yyyy-MM-dd HH:mm"
     */
    fun formatShort(timestampMs: Long, zone: ZoneId = defaultZone): String =
        toLocalDateTime(timestampMs, zone).format(shortFormatter)

    /**
     * 将时间戳格式化为 "yyyy-MM-dd"
     */
    fun formatDate(timestampMs: Long, zone: ZoneId = defaultZone): String =
        toLocalDateTime(timestampMs, zone).format(dateFormatter)

    /**
     * 将时间戳格式化为 "HH:mm:ss"
     */
    fun formatTime(timestampMs: Long, zone: ZoneId = defaultZone): String =
        toLocalDateTime(timestampMs, zone).format(timeFormatter)

    /**
     * 当前时间戳（毫秒）
     */
    fun nowMillis(): Long = System.currentTimeMillis()

    /**
     * 当前 ISO 时间字符串
     */
    fun nowIsoString(zone: ZoneId = defaultZone): String =
        ZonedDateTime.now(zone).format(isoFormatter)

    /**
     * 获取相对时间描述（如 "3分钟前", "2小时前"）
     */
    fun getRelativeTime(timestampMs: Long): String {
        val diff = nowMillis() - timestampMs
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> formatShort(timestampMs)
        }
    }
}
