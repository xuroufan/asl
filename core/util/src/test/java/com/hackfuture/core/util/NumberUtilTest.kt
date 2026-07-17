package com.hackfuture.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** NumberUtil 单元测试 */
class NumberUtilTest {

    @Test
    fun `formatPrice formats with two decimals and comma`() {
        assertEquals("12,345.68", NumberUtil.formatPrice(12345.678))
        assertEquals("0.00", NumberUtil.formatPrice(0.0))
        assertEquals("-100.50", NumberUtil.formatPrice(-100.5))
    }

    @Test
    fun `formatQuantity removes trailing zeros`() {
        assertEquals("1.5", NumberUtil.formatQuantity(1.50000000))
        assertEquals("0.000001", NumberUtil.formatQuantity(0.000001))
        assertEquals("100", NumberUtil.formatQuantity(100.0))
    }

    @Test
    fun `formatPercent shows sign and percent`() {
        assertEquals("+5.00%", NumberUtil.formatPercent(5.0))
        assertEquals("-3.50%", NumberUtil.formatPercent(-3.5))
        assertEquals("+0.00%", NumberUtil.formatPercent(0.0))
    }

    @Test
    fun `formatAmount includes comma separator`() {
        assertEquals("1,234,567.00", NumberUtil.formatAmount(1234567.0))
        assertEquals("0.00", NumberUtil.formatAmount(0.0))
    }

    @Test
    fun `round rounds half up`() {
        assertEquals(3.14, NumberUtil.round(3.14159, 2), 0.0001)
        assertEquals(3.142, NumberUtil.round(3.14159, 3), 0.0001)
        assertEquals(100.0, NumberUtil.round(99.999, 2), 0.01)
    }

    @Test
    fun `truncate truncates without rounding`() {
        assertEquals(3.14, NumberUtil.truncate(3.14999, 2), 0.0001)
        assertEquals(3.14, NumberUtil.truncate(3.14159, 2), 0.0001)
    }

    @Test
    fun `formatCompactChinese formats wan-level`() {
        assertEquals("1.23万", NumberUtil.formatCompactChinese(12345.0))
    }

    @Test
    fun `formatCompactChinese formats yi-level`() {
        assertEquals("1.23亿", NumberUtil.formatCompactChinese(123450000.0))
    }

    @Test
    fun `formatCompactChinese formats below wan as normal price`() {
        assertEquals("9,876.00", NumberUtil.formatCompactChinese(9876.0))
    }

    @Test
    fun `round defaults to 2 decimal places`() {
        assertEquals(1.23, NumberUtil.round(1.234999), 0.0001)
    }
}
