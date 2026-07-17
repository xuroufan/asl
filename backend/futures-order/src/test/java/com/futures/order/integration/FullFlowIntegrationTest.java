package com.futures.order.integration;

import com.futures.common.exception.BizException;
import com.futures.common.result.Result;
import com.futures.order.config.OrderStateMachine;
import com.futures.order.dto.OrderMatchEvent;
import com.futures.order.dto.OrderPlaceRequest;
import com.futures.order.dto.OrderVO;
import com.futures.order.dto.PlaceOrderEvent;
import com.futures.order.entity.OrderEntity;
import com.futures.order.enums.OrderStatus;
import com.futures.order.feign.FundFeignClient;
import com.futures.order.feign.RiskFeignClient;
import com.futures.order.mapper.OrderMapper;
import com.futures.order.service.OrderEventProducer;
import com.futures.order.service.OrderIdempotencyService;
import com.futures.order.service.OrderSeataTccService;
import com.futures.order.service.OrderService;
import com.futures.order.service.OrderTccService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 端到端集成测试 — 模拟完整交易流程
 * <p>
 * 测试覆盖：
 * <pre>
 *   下单 -> 验资/风控 -> 撮合 -> 成交回报 -> 持仓更新/资金变动
 * </pre>
 * 使用 Mock 模拟外部服务（fund-service / risk-service / matching-engine）
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    private static final Long USER_ID = 10001L;
    private static final String SYMBOL = "ES";
    private static final BigDecimal ENTRY_PRICE = BigDecimal.valueOf(4500.0);
    private static final int VOLUME = 1;

    @Mock private OrderMapper orderMapper;
    @Mock private OrderIdempotencyService idempotencyService;
    @Mock private FundFeignClient fundFeignClient;
    @Mock private RiskFeignClient riskFeignClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private OrderStateMachine stateMachine;
    private OrderEventProducer eventProducer;
    private OrderTccService tccService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
        eventProducer = spy(new OrderEventProducer(eventPublisher));
        tccService = new OrderSeataTccService(fundFeignClient, riskFeignClient);
        orderService = new OrderService(
                orderMapper, stateMachine, idempotencyService, eventProducer, tccService);

        lenient().when(idempotencyService.tryMarkProcessed(anyString())).thenReturn(true);

        lenient().when(orderMapper.insert(any(OrderEntity.class)))
                .thenAnswer(inv -> {
                    OrderEntity e = inv.getArgument(0);
                    e.setId(1L);
                    return 1;
                });
    }

    // ==================== 场景1-5：下单全流程 ====================

    @Test
    @Order(1)
    @DisplayName("正常下单：验资验仓全部通过 -> 订单PENDING -> MQ事件发送")
    void testPlaceOrder_SuccessFlow() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), eq(ENTRY_PRICE)))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        OrderPlaceRequest req = buildBuyRequest();
        OrderVO result = orderService.placeOrder(req);

        assertNotNull(result);
        assertEquals("BUY", result.getDirection());
        assertEquals("LIMIT", result.getOrderType());
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getOrderId().startsWith("ORD"));

        verify(riskFeignClient).checkPositionLimit(USER_ID, SYMBOL, VOLUME);
        verify(fundFeignClient).freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE);
        verify(eventProducer).publishPlaceOrder(any(PlaceOrderEvent.class));
        verify(orderMapper).insert(any(OrderEntity.class));
    }

    @Test
    @Order(2)
    @DisplayName("资金不足：冻结失败 -> 下单回滚")
    void testPlaceOrder_InsufficientFunds_ShouldRollback() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), eq(ENTRY_PRICE)))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.error(400, "资金不足"));
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        BizException ex = assertThrows(BizException.class,
                () -> orderService.placeOrder(buildBuyRequest()));
        assertTrue(ex.getMessage().contains("资金"));
        verify(orderMapper, never()).insert(any(OrderEntity.class));
    }

    @Test
    @Order(3)
    @DisplayName("风控拒绝：持仓超限 -> 不下单不冻结")
    void testPlaceOrder_PositionLimitExceeded_ShouldReject() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.error(400, "持仓超限"));

        assertThrows(BizException.class,
                () -> orderService.placeOrder(buildBuyRequest()));
        verify(fundFeignClient, never()).freezeMargin(any(), any(), any(), any());
        verify(orderMapper, never()).insert(any(OrderEntity.class));
    }

    @Test
    @Order(4)
    @DisplayName("日亏损超限：冻结后回滚解冻")
    void testPlaceOrder_DailyLossLimitBreached_ShouldUnfreeze() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), eq(ENTRY_PRICE)))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.error(400, "日内亏损已超限额"));

        assertThrows(BizException.class,
                () -> orderService.placeOrder(buildBuyRequest()));
        verify(fundFeignClient).freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE);
        verify(fundFeignClient).unfreezeMargin(USER_ID, SYMBOL, VOLUME);
        verify(orderMapper, never()).insert(any(OrderEntity.class));
    }

    @Test
    @Order(5)
    @DisplayName("市价单下单成功")
    void testPlaceMarketOrder_Success() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), any()))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(USER_ID);
        req.setSymbol(SYMBOL);
        req.setDirection("SELL");
        req.setOrderType("MARKET");
        req.setVolume(VOLUME);
        req.setPrice(ENTRY_PRICE);

        OrderVO r = orderService.placeOrder(req);
        assertEquals("MARKET", r.getOrderType());
    }

    @Test
    @Order(6)
    @DisplayName("止损单下单成功")
    void testPlaceStopOrder_Success() {
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), any()))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(USER_ID);
        req.setSymbol(SYMBOL);
        req.setDirection("BUY");
        req.setOrderType("STOP");
        req.setPrice(ENTRY_PRICE);
        req.setVolume(VOLUME);
        req.setStopPrice(BigDecimal.valueOf(4450));

        OrderVO r = orderService.placeOrder(req);
        assertEquals("STOP", r.getOrderType());
    }

    // ==================== 场景7-9：成交回报 ====================

    @Test
    @Order(7)
    @DisplayName("撮合成交：PENDING -> PARTIAL -> FILLED 全流程")
    void testOrderMatchingFlow_FullLifecycle() {
        // 1) 创建订单
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, 2))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(2), eq(ENTRY_PRICE)))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, 2, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(USER_ID);
        req.setSymbol(SYMBOL);
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(ENTRY_PRICE);
        req.setVolume(2);

        OrderVO placed = orderService.placeOrder(req);
        assertEquals("PENDING", placed.getStatus());

        // 2) 部分成交1手
        OrderEntity entity = new OrderEntity();
        entity.setId(placed.getId());
        entity.setOrderId(placed.getOrderId());
        entity.setUserId(USER_ID);
        entity.setSymbol(SYMBOL);
        entity.setDirection(com.futures.order.enums.OrderDirection.BUY);
        entity.setOrderType(com.futures.order.enums.OrderType.LIMIT);
        entity.setPrice(ENTRY_PRICE);
        entity.setVolume(2);
        entity.setFilledVolume(0);
        entity.setAvgPrice(BigDecimal.ZERO);
        entity.setStatus(OrderStatus.PENDING);

        when(orderMapper.selectOne(any())).thenReturn(entity);
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.handleMatchEvent(OrderMatchEvent.builder()
                .orderId(placed.getOrderId()).matchVolume(1)
                .matchPrice(ENTRY_PRICE).totalFilledVolume(1)
                .avgPrice(ENTRY_PRICE).newStatus("PARTIAL")
                .matchedAt(LocalDateTime.now()).build());

        ArgumentCaptor<OrderEntity> c1 = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).updateById(c1.capture());
        assertEquals(OrderStatus.PARTIAL, c1.getValue().getStatus());
        assertEquals(1, c1.getValue().getFilledVolume());

        // 3) 全部成交1手
        entity.setFilledVolume(1);
        entity.setStatus(OrderStatus.PARTIAL);

        OrderMatchEvent fullEvent = OrderMatchEvent.builder()
                .orderId(placed.getOrderId()).matchVolume(1)
                .matchPrice(ENTRY_PRICE).totalFilledVolume(2)
                .avgPrice(ENTRY_PRICE).newStatus("FILLED")
                .matchedAt(LocalDateTime.now()).build();

        orderService.handleMatchEvent(fullEvent);

        verify(orderMapper, times(2)).updateById(c1.capture());
        assertEquals(OrderStatus.FILLED, c1.getValue().getStatus());
        assertEquals(2, c1.getValue().getFilledVolume());
    }

    // ==================== 场景10-12：撤单 ====================

    @Test
    @Order(8)
    @DisplayName("撤单成功：PENDING -> CANCELLED")
    void testCancelOrder_Success() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId("ORD_CANCEL");
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setVolume(5);
        order.setFilledVolume(0);

        when(orderMapper.selectById(1L)).thenReturn(order);
        orderService.cancelOrder(1L, USER_ID);

        verify(orderMapper).updateById(argThat(e -> e.getStatus() == OrderStatus.CANCELLED));
    }

    @Test
    @Order(9)
    @DisplayName("撤单拒绝：FILLED订单不可撤")
    void testCancelOrder_FilledOrder_ShouldThrow() {
        OrderEntity o = new OrderEntity();
        o.setId(2L); o.setUserId(USER_ID); o.setStatus(OrderStatus.FILLED);
        when(orderMapper.selectById(2L)).thenReturn(o);
        assertThrows(BizException.class, () -> orderService.cancelOrder(2L, USER_ID));
    }

    @Test
    @Order(10)
    @DisplayName("撤单拒绝：非本人订单不可撤")
    void testCancelOrder_WrongUser_ShouldThrow() {
        OrderEntity o = new OrderEntity();
        o.setId(3L); o.setUserId(999L); o.setStatus(OrderStatus.PENDING);
        when(orderMapper.selectById(3L)).thenReturn(o);
        assertThrows(BizException.class, () -> orderService.cancelOrder(3L, USER_ID));
    }

    // ==================== 场景13-15：参数校验 ====================

    @Test
    @Order(11)
    @DisplayName("校验拒绝：手数为0")
    void testPlaceOrder_VolumeZero_ShouldThrow() {
        OrderPlaceRequest req = buildBuyRequest();
        req.setVolume(0);
        assertThrows(BizException.class, () -> orderService.placeOrder(req));
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @Order(12)
    @DisplayName("校验拒绝：限价单价格为0")
    void testPlaceOrder_PriceZero_ShouldThrow() {
        OrderPlaceRequest req = buildBuyRequest();
        req.setPrice(BigDecimal.ZERO);
        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    @Test
    @Order(13)
    @DisplayName("校验拒绝：无效合约")
    void testPlaceOrder_InvalidSymbol_ShouldThrow() {
        OrderPlaceRequest req = buildBuyRequest();
        req.setSymbol("BAD_SYM");
        BizException ex = assertThrows(BizException.class, () -> orderService.placeOrder(req));
        assertTrue(ex.getMessage().contains("合约"));
    }

    // ==================== 场景16：幂等性 ====================

    @Test
    @Order(14)
    @DisplayName("幂等校验：重复clientOrderId返回已存在订单")
    void testIdempotency_DuplicateClientOrderId() {
        String cid = "CLIENT_ORDER_001";
        when(idempotencyService.getExistingOrderId(cid)).thenReturn(100L);
        OrderEntity existing = new OrderEntity();
        existing.setId(100L);
        existing.setOrderId("ORD_EXISTING");
        existing.setUserId(USER_ID);
        existing.setSymbol(SYMBOL);
        existing.setDirection(com.futures.order.enums.OrderDirection.BUY);
        existing.setOrderType(com.futures.order.enums.OrderType.LIMIT);
        existing.setPrice(ENTRY_PRICE);
        existing.setVolume(VOLUME);
        existing.setFilledVolume(1);
        existing.setStatus(OrderStatus.FILLED);
        existing.setAvgPrice(ENTRY_PRICE);
        existing.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(orderMapper.selectById(100L)).thenReturn(existing);

        OrderPlaceRequest req = buildBuyRequest();
        req.setClientOrderId(cid);
        OrderVO r = orderService.placeOrder(req);
        assertEquals("ORD_EXISTING", r.getOrderId());
        verify(orderMapper, never()).insert(any(OrderEntity.class));
    }

    // ==================== 场景17：完整交易生命周期 ====================

    @Test
    @Order(15)
    @DisplayName("完整交易生命周期：下单 -> 成交 -> 查询")
    void testFullTradeLifecycle() {
        // 下单
        when(riskFeignClient.checkPositionLimit(USER_ID, SYMBOL, VOLUME))
                .thenReturn(Result.success());
        when(riskFeignClient.checkMargin(eq(USER_ID), eq(SYMBOL), eq(VOLUME), eq(ENTRY_PRICE)))
                .thenReturn(Result.success());
        when(fundFeignClient.freezeMargin(USER_ID, SYMBOL, VOLUME, ENTRY_PRICE))
                .thenReturn(Result.success());
        when(riskFeignClient.checkDailyLossLimit(USER_ID))
                .thenReturn(Result.success());

        OrderVO order = orderService.placeOrder(buildBuyRequest());
        assertEquals("PENDING", order.getStatus());

        // 模拟成交
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setOrderId(order.getOrderId());
        entity.setUserId(USER_ID);
        entity.setSymbol(SYMBOL);
        entity.setDirection(com.futures.order.enums.OrderDirection.BUY);
        entity.setOrderType(com.futures.order.enums.OrderType.LIMIT);
        entity.setPrice(ENTRY_PRICE);
        entity.setVolume(VOLUME);
        entity.setFilledVolume(0);
        entity.setAvgPrice(BigDecimal.ZERO);
        entity.setStatus(OrderStatus.PENDING);

        when(orderMapper.selectOne(any())).thenReturn(entity);
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.handleMatchEvent(OrderMatchEvent.builder()
                .orderId(order.getOrderId()).matchVolume(1)
                .matchPrice(ENTRY_PRICE).totalFilledVolume(1)
                .avgPrice(ENTRY_PRICE).newStatus("FILLED")
                .matchedAt(LocalDateTime.now()).build());

        // 查询
        when(orderMapper.selectById(order.getId())).thenReturn(entity);
        OrderVO detail = orderService.getOrderDetail(order.getId());
        assertNotNull(detail);
    }

    // ==================== 工具方法 ====================

    private OrderPlaceRequest buildBuyRequest() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(USER_ID);
        req.setSymbol(SYMBOL);
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(ENTRY_PRICE);
        req.setVolume(VOLUME);
        return req;
    }
}
