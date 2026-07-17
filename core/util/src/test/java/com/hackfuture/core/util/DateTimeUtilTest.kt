package com.hackfuture.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** DateTimeUtil 单元测试 */
class DateTimeUtilTest {

    // 2026-07-17 12:00:00 Asia/Shanghai
    private val testTimestampMs = 1784260800000L

    @Test
    fun `toLocalDateTime converts timestamp correctly`() {
        val ldt = DateTimeUtil.toLocalDateTime(testTimestampMs)
        assertEquals(2026, ldt.year)
        assertEquals(7, ldt.monthValue)
        assertEquals(17, ldt.dayOfMonth)
    }

    @Test
    fun `toLocalDateTime with custom zoneid`() {
        val utc = ZoneId.of("UTC")
        val ldt = DateTimeUtil.toLocalDateTime(testTimestampMs, utc)
        assertEquals(2026, ldt.year)
        assertEquals(7, ldt.monthValue)
        assertEquals(17, ldt.dayOfMonth)
        assertEquals(4, ldt.hour) // UTC = Shanghai - 8
    }

    @Test
    fun `formatShort returns yyyy-MM-dd HHmm pattern`() {
        val result = DateTimeUtil.formatShort(testTimestampMs)
        assertTrue(result.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""")))
        assertTrue(result.contains("2026-07-17"))
    }

    @Test
    fun `formatDate returns yyyy-MM-dd`() {
        val result = DateTimeUtil.formatDate(testTimestampMs)
        assertTrue(result.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertEquals("2026-07-17", result)
    }

    @Test
    fun `formatTime returns time pattern`() {
        val result = DateTimeUtil.formatTime(testTimestampMs)
        assertTrue(result.matches(Regex("""\d{2}:\d{2}:\d{2}""")))
    }

    @Test
    fun `nowMillis returns positive value`() {
        assertTrue(DateTimeUtil.nowMillis() > 0)
    }

    @Test
    fun `nowIsoString returns parsable ISO datetime`() {
        val iso = DateTimeUtil.nowIsoString()
        assertTrue(iso.isNotEmpty())
        assertTrue(iso.contains("T"))
    }

    @Test
    fun `getRelativeTime returns just now for recent`() {
        val ts = DateTimeUtil.nowMillis() - 10_000
        assertEquals("刚刚", DateTimeUtil.getRelativeTime(ts))
    }

    @Test
    fun `getRelativeTime returns minutes ago`() {
        val ts = DateTimeUtil.nowMillis() - 300_000
        assertEquals("5分钟前", DateTimeUtil.getRelativeTime(ts))
    }

    @Test
    fun `getRelativeTime returns hours ago`() {
        val ts = DateTimeUtil.nowMillis() - 7_200_000
        assertEquals("2小时前", DateTimeUtil.getRelativeTime(ts))
    }

    @Test
    fun `getRelativeTime returns days ago`() {
        val ts = DateTimeUtil.nowMillis() - 345_600_000
        assertEquals("4天前", DateTimeUtil.getRelativeTime(ts))
    }

    @Test
    fun `getRelativeTime falls back to formatShort for old timestamps`() {
        val ts = 1000000L // 1970-01-01
        val result = DateTimeUtil.getRelativeTime(ts)
        // Should contain year separator
        assertTrue(result.contains("-") || result.contains("/"))
    }
}
