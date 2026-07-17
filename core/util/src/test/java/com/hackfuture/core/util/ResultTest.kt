package com.hackfuture.core.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/** Result sealed class 单元测试 — 覆盖全部操作符 */
class ResultTest {

    @Test
    fun `success creates Success with data`() {
        val result: Result<Int> = Result.success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `error creates Error with exception and optional message`() {
        val exc = RuntimeException("boom")
        val result: Result<Nothing> = Result.error(exc, "custom message")
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
        assertEquals("custom message", (result as Result.Error).message)
    }

    @Test
    fun `error defaults message to null`() {
        val result: Result<Nothing> = Result.error(RuntimeException("boom"))
        assertNull((result as Result.Error).message)
    }

    @Test
    fun `runCatching returns Success on normal block`() {
        val result = Result.runCatching { 1 + 1 }
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun `runCatching catches exception and returns Error`() {
        val result: Result<Int> = Result.runCatching { throw IllegalArgumentException("bad") }
        assertTrue(result.isError)
        assertEquals("bad", (result as Result.Error).message)
    }

    @Test
    fun `getOrNull returns data on Success, null on Error`() {
        assertEquals("hello", Result.success("hello").getOrNull())
        val error: Result<String> = Result.error(Exception())
        assertNull(error.getOrNull())
    }

    @Test
    fun `getOrDefault returns data on Success, default on Error`() {
        assertEquals(10, Result.success(10).getOrDefault(0))
        val error: Result<Int> = Result.error(Exception())
        assertEquals(-1, error.getOrDefault(-1))
    }

    @Test
    fun `map transforms Success and passes through Error`() {
        assertEquals(10, Result.success(5).map { it * 2 }.getOrNull())
        val error: Result<Int> = Result.error(Exception("fail"))
        assertTrue(error.map { it + 1 }.isError)
    }

    @Test
    fun `flatMap chains Success and short-circuits on Error`() {
        assertEquals(9, Result.success(3).flatMap { Result.success(it * 3) }.getOrNull())
        val error: Result<Int> = Result.error(Exception("fail"))
        assertTrue(error.flatMap { Result.success(99) }.isError)
    }

    @Test
    fun `flatMap propagates inner Error`() {
        assertTrue(Result.success(1).flatMap { val r: Result<Int> = Result.error(Exception("inner")); r }.isError)
    }

    @Test
    fun `onSuccess called for Success, skipped for Error`() {
        var captured = 0
        Result.success(7).onSuccess { captured = it }
        assertEquals(7, captured)
        var called = false
        val error: Result<Int> = Result.error(Exception())
        error.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onError called for Error, skipped for Success`() {
        var captured: String? = null
        val error: Result<Int> = Result.error(RuntimeException("oops"), "msg")
        error.onError { _, msg -> captured = msg }
        assertEquals("msg", captured)
        var called = false
        Result.success(1).onError { _, _ -> called = true }
        assertFalse(called)
    }

    @Test
    fun `requireValue returns data for Success and throws`() = runBlocking {
        assertEquals("ok", Result.success("ok").requireValue())
        try {
            val error: Result<Int> = Result.error(IllegalStateException("nope"))
            error.requireValue()
            org.junit.Assert.fail("should have thrown")
        } catch (_: Throwable) { }
    }

    @Test
    fun `chained map and onSuccess works end to end`() {
        var result = 0
        Result.success(10).map { it * 2 }.onSuccess { result = it }
        assertEquals(20, result)
    }

    @Test
    fun `error with null message does not throw`() {
        val result: Result<Nothing> = Result.error(NullPointerException())
        assertEquals(NullPointerException::class, (result as Result.Error).exception::class)
    }
}
