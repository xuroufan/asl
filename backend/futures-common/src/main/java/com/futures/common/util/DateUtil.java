package com.futures.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 */
public class DateUtil {

    private DateUtil() {}

    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FORMATTER_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMATTER_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 获取当前时间字符串 yyyy-MM-dd HH:mm:ss */
    public static String nowStr() {
        return LocalDateTime.now().format(FORMATTER_DATETIME);
    }

    /** 获取当前日期字符串 yyyy-MM-dd */
    public static String todayStr() {
        return LocalDate.now().format(FORMATTER_DATE);
    }

    /** LocalDateTime 转字符串 */
    public static String format(LocalDateTime dt) {
        return dt == null ? "" : dt.format(FORMATTER_DATETIME);
    }

    /** 字符串转 LocalDateTime */
    public static LocalDateTime parse(String str) {
        return LocalDateTime.parse(str, FORMATTER_DATETIME);
    }

    /** 获取当前时间戳（毫秒） */
    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    /** 判断是否在交易时间段（中国期货日盘 9:00-15:00，夜盘 21:00-23:00） */
    public static boolean isTradingTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        int h = now.getHour();
        int m = now.getMinute();
        int time = h * 100 + m;
        return (time >= 900 && time <= 1500) || (time >= 2100 || time <= 2300);
    }
}
