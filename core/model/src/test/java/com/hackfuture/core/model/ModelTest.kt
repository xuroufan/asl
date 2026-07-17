package com.hackfuture.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 领域模型单元测试 — 覆盖全部 computed property */
class ModelTest {

    // ==================== Order ====================

    @Test
    fun `order remainingQuantity`() {
        val o = Order(id="1", symbol="BTC", type=OrderType.LIMIT, side=OrderSide.BUY,
            status=OrderStatus.PARTIALLY_FILLED, price=100.0, quantity=10.0, filledQuantity=3.0,
            totalAmount=1000.0, createdAt=0L, updatedAt=0L)
        assertEquals(7.0, o.remainingQuantity, 0.0001)
    }

    @Test
    fun `order isFullyFilled`() {
        assertTrue(Order(id="1", symbol="S", type=OrderType.MARKET, side=OrderSide.BUY,
            status=OrderStatus.FILLED, quantity=1.0, totalAmount=100.0, createdAt=0L, updatedAt=0L).isFullyFilled)
        assertFalse(Order(id="2", symbol="S", type=OrderType.MARKET, side=OrderSide.BUY,
            status=OrderStatus.PENDING, quantity=1.0, totalAmount=100.0, createdAt=0L, updatedAt=0L).isFullyFilled)
    }

    @Test
    fun `order isActive for pending and partially filled`() {
        assertTrue(Order(id="1", symbol="S", type=OrderType.LIMIT, side=OrderSide.BUY,
            status=OrderStatus.PENDING, quantity=1.0, totalAmount=100.0, createdAt=0L, updatedAt=0L).isActive)
        assertTrue(Order(id="2", symbol="S", type=OrderType.LIMIT, side=OrderSide.BUY,
            status=OrderStatus.PARTIALLY_FILLED, quantity=1.0, totalAmount=100.0, createdAt=0L, updatedAt=0L).isActive)
        assertFalse(Order(id="3", symbol="S", type=OrderType.LIMIT, side=OrderSide.BUY,
            status=OrderStatus.FILLED, quantity=1.0, totalAmount=100.0, createdAt=0L, updatedAt=0L).isActive)
    }

    @Test
    fun `order remainingQuantity zero when filled`() {
        val o = Order(id="1", symbol="S", type=OrderType.MARKET, side=OrderSide.BUY,
            status=OrderStatus.FILLED, quantity=5.0, filledQuantity=5.0, totalAmount=500.0, createdAt=0L, updatedAt=0L)
        assertEquals(0.0, o.remainingQuantity, 0.0001)
    }

    // ==================== Position ====================

    @Test
    fun `position marketValue`() {
        val p = Position(id="1", symbol="BTC", side=PositionSide.LONG, quantity=2.0,
            entryPrice=100.0, currentPrice=150.0, unrealizedPnl=100.0, unrealizedPnlPercent=50.0,
            realizedPnl=0.0, marginUsed=200.0, openedAt=0L, updatedAt=0L)
        assertEquals(300.0, p.marketValue, 0.0001)
    }

    @Test
    fun `position pnlDirection`() {
        val up = Position(id="1", symbol="S", side=PositionSide.LONG, quantity=1.0,
            entryPrice=100.0, currentPrice=150.0, unrealizedPnl=50.0, unrealizedPnlPercent=50.0,
            realizedPnl=0.0, marginUsed=100.0, openedAt=0L, updatedAt=0L)
        assertEquals(PriceDirection.UP, up.pnlDirection)

        val down = up.copy(unrealizedPnl = -30.0)
        assertEquals(PriceDirection.DOWN, down.pnlDirection)

        val flat = up.copy(unrealizedPnl = 0.0)
        assertEquals(PriceDirection.FLAT, flat.pnlDirection)
    }

    // ==================== MarketData ====================

    @Test
    fun `marketData priceDirection`() {
        val base = MarketData(symbol="BTC", name="", exchange="", price=100.0,
            change=0.0, changePercent=0.0, high=110.0, low=90.0, open=100.0,
            close=100.0, volume=1000L, timestamp=0L)
        assertEquals(PriceDirection.FLAT, base.priceDirection)
        assertEquals(PriceDirection.UP, base.copy(change = 5.0).priceDirection)
        assertEquals(PriceDirection.DOWN, base.copy(change = -3.0).priceDirection)
    }

    // ==================== Quote ====================

    @Test
    fun `quote direction`() {
        val q = Quote(symbol="BTC", lastPrice=100.0, change=0.0, changePercent=0.0,
            open=100.0, high=105.0, low=95.0, volume=1000L)
        assertEquals(PriceDirection.FLAT, q.direction)
        assertEquals(PriceDirection.UP, q.copy(change = 10.0).direction)
        assertEquals(PriceDirection.DOWN, q.copy(change = -5.0).direction)
    }

    // ==================== User ====================

    @Test
    fun `user isVerified based on kycLevel`() {
        val u = User(id="1", username="t", displayName="T", email="t@t.com",
            createdAt=0L, updatedAt=0L)
        assertFalse(u.isVerified)
        assertTrue(u.copy(kycLevel = KycLevel.BASIC).isVerified)
        assertTrue(u.copy(kycLevel = KycLevel.ADVANCED).isVerified)
    }

    @Test
    fun `user isActive`() {
        val u = User(id="1", username="t", displayName="T", email="t@t.com",
            createdAt=0L, updatedAt=0L)
        assertTrue(u.isActive)
        assertFalse(u.copy(accountStatus = AccountStatus.SUSPENDED).isActive)
        assertFalse(u.copy(accountStatus = AccountStatus.FROZEN).isActive)
        assertFalse(u.copy(accountStatus = AccountStatus.CLOSED).isActive)
    }

    // ==================== AccountBalance ====================

    @Test
    fun `accountBalance available`() {
        val b = AccountBalance(asset="USDT", free=100.0, locked=50.0, total=150.0)
        assertEquals(100.0, b.available, 0.0001)
    }

    // ==================== PageResponse ====================

    @Test
    fun `pageResponse hasMore`() {
        val p1 = PageResponse(items=emptyList<Int>(), total=0, page=1, size=20, totalPages=1)
        assertFalse(p1.hasMore)
        val p2 = PageResponse(items=emptyList<Int>(), total=30, page=1, size=20, totalPages=2)
        assertTrue(p2.hasMore)
    }

    @Test
    fun `pageResponse isEmpty`() {
        assertTrue(PageResponse(items=emptyList<Int>(), total=0, page=1, size=20, totalPages=0).isEmpty)
        assertFalse(PageResponse(items=listOf(1), total=1, page=1, size=20, totalPages=1).isEmpty)
    }

    @Test
    fun `pageResponse empty companion`() {
        val empty = PageResponse.empty<Int>()
        assertTrue(empty.isEmpty)
        assertFalse(empty.hasMore)
        assertEquals(1, empty.page)
    }

    // ==================== ApiResult.parseData ====================

    @Test
    fun `parseData returns null when data is null`() {
        val result = ApiResult(code=200, msg="ok", data=null)
        assertNull(result.parseData<User>(Json { ignoreUnknownKeys = true }))
    }

    @Test
    fun `parseData returns parsed object when data is valid JSON`() {
        val json = buildJsonObject {
            put("id", JsonPrimitive("1"))
            put("username", JsonPrimitive("test"))
            put("displayName", JsonPrimitive("Test"))
            put("email", JsonPrimitive("t@t.com"))
            put("createdAt", JsonPrimitive(0))
            put("updatedAt", JsonPrimitive(0))
        }
        val result = ApiResult(code=200, msg="ok", data=json)
        val user = result.parseData<User>(Json { ignoreUnknownKeys = true })
        assertTrue(user != null)
        assertEquals("test", user?.username)
    }

    @Test
    fun `parseData returns null when type mismatch`() {
        val json = buildJsonObject {
            put("id", JsonPrimitive("1"))
        }
        val result = ApiResult(code=200, msg="ok", data=json)
        assertNull(result.parseData<List<Int>>(Json { ignoreUnknownKeys = true }))
    }

    // ==================== Side-effect free enum defaults ====================

    @Test
    fun `all enums can be instantiated with defaults`() {
        assertEquals("MARKET", OrderType.MARKET.name)
        assertEquals("BUY", OrderSide.BUY.name)
        assertEquals("FILLED", OrderStatus.FILLED.name)
        assertEquals("LONG", PositionSide.LONG.name)
        assertEquals("UP", PriceDirection.UP.name)
        assertEquals("STOCK", QuoteCategory.STOCK.name)
        assertEquals("DEPOSIT", TransactionType.DEPOSIT.name)
        assertEquals("PENDING", TransactionStatus.PENDING.name)
        assertEquals("ASC", SortOrder.ASC.name)
    }
}
