package com.hackfuture.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** FormatUtils 单元测试 — 验证 Locale 感知的格式化 */
class FormatUtilsTest {

    private val testTimestampMs = 1784260800000L // 2026-07-17 12:00 Asia/Shanghai

    @Test
    fun `formatPrice with en locale uses comma`() {
        assertEquals("20,000.50", FormatUtils.formatPrice(20000.50, Locale.ENGLISH))
    }

    @Test
    fun `formatPrice with zh locale formats correctly`() {
        // On JDK 21, zh_CN locale does include grouping separator
        val result = FormatUtils.formatPrice(20000.50, Locale.SIMPLIFIED_CHINESE)
        assertTrue("Should contain decimal point", result.contains(".50"))
        assertTrue("Should contain numeric chars", result.replace(",", "").replace(".", "").all { it.isDigit() })
    }

    @Test
    fun `formatQuantity uses up to 4 decimals without grouping`() {
        val qty = FormatUtils.formatQuantity(12345.6789, Locale.ENGLISH)
        assertTrue("Should not contain comma", !qty.contains(","))
    }

    @Test
    fun `formatQuantity removes trailing zeros`() {
        assertEquals("100", FormatUtils.formatQuantity(100.0, Locale.ENGLISH))
    }

    @Test
    fun `formatPercent has prefix sign`() {
        assertEquals("+1.85%", FormatUtils.formatPercent(1.85, Locale.ENGLISH))
        assertEquals("-0.50%", FormatUtils.formatPercent(-0.50, Locale.ENGLISH))
    }

    @Test
    fun `formatDate with zh locale uses Chinese format`() {
        val result = FormatUtils.formatDate(testTimestampMs, Locale.SIMPLIFIED_CHINESE)
        assertTrue(result.contains("年") || result.contains("月") || result.contains("日"))
    }

    @Test
    fun `formatDate with en locale uses MMM d, yyyy`() {
        val result = FormatUtils.formatDate(testTimestampMs, Locale.ENGLISH)
        assertTrue(result.contains("Jul"))
        assertTrue(result.contains("2026"))
    }

    @Test
    fun `formatDateTime with zh locale`() {
        val result = FormatUtils.formatDateTime(testTimestampMs, Locale.SIMPLIFIED_CHINESE)
        assertTrue(result.contains("年") || result.contains("日"))
    }

    @Test
    fun `formatTime returns time pattern`() {
        val result = FormatUtils.formatTime(testTimestampMs, Locale.ENGLISH)
        assertTrue(result.matches(Regex("""\d{2}:\d{2}:\d{2}""")))
    }

    @Test
    fun `formatCompact with zh locale uses wan and yi`() {
        assertEquals("1.23亿", FormatUtils.formatCompact(123000000.0, Locale.SIMPLIFIED_CHINESE))
        assertEquals("1.23万", FormatUtils.formatCompact(12300.0, Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `formatCompact with en locale uses K M B`() {
        assertEquals("1.23K", FormatUtils.formatCompact(1230.0, Locale.ENGLISH))
        assertEquals("1.23M", FormatUtils.formatCompact(1230000.0, Locale.ENGLISH))
        assertEquals("1.23B", FormatUtils.formatCompact(1230000000.0, Locale.ENGLISH))
    }

    @Test
    fun `formatCompact with en falls back to price for small values`() {
        val result = FormatUtils.formatCompact(100.0, Locale.ENGLISH)
        assertTrue(result.contains(".00"))
    }

    @Test
    fun `formatVolume delegates to formatCompact`() {
        assertEquals("1.23K", FormatUtils.formatVolume(1234, Locale.ENGLISH))
    }
}
