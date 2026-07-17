package com.futures.order.service;

import com.futures.common.exception.BizException;
import com.futures.order.config.OrderStateMachine;
import com.futures.order.dto.OrderMatchEvent;
import com.futures.order.dto.OrderPlaceRequest;
import com.futures.order.dto.OrderVO;
import com.futures.order.entity.OrderEntity;
import com.futures.order.enums.OrderDirection;
import com.futures.order.enums.OrderStatus;
import com.futures.order.enums.OrderType;
import com.futures.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 */
class OrderServiceTest {

    private OrderMapper orderMapper;
    private OrderIdempotencyService idempotencyService;
    private OrderEventProducer eventProducer;
    private OrderTccService seataTccService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        eventProducer = new OrderEventProducer(mock(ApplicationEventPublisher.class));
        seataTccService = mock(OrderTccService.class);
        doNothing().when(seataTccService).tryPlace(any(), any());

        // 幂等服务：总是通过
        idempotencyService = new OrderIdempotencyService(null) {
            @Override public boolean tryMarkProcessed(String clientOrderId) { return true; }
            @Override public Long getExistingOrderId(String clientOrderId) { return null; }
            @Override public void bindOrderId(String clientOrderId, Long orderId) {}
        };

        orderService = new OrderService(
                orderMapper,
                new OrderStateMachine(),
                idempotencyService,
                eventProducer,
                seataTccService);
    }

    // ==================== 创建订单测试 ====================

    @Test
    void testPlaceLimitOrder_Success() {
        when(orderMapper.insert(any())).thenAnswer(inv -> {
            ((OrderEntity) inv.getArgument(0)).setId(1L);
            return 1;
        });

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(4500.0));
        req.setVolume(2);

        OrderVO result = orderService.placeOrder(req);

        assertNotNull(result);
        assertEquals("BUY", result.getDirection());
        assertEquals("LIMIT", result.getOrderType());
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getOrderId().startsWith("ORD"));
        assertEquals(2, result.getVolume().intValue());

        verify(orderMapper).insert(argThat(e -> {
            OrderEntity entity = (OrderEntity) e;
            return entity.getSymbol().equals("ES")
                    && entity.getDirection() == OrderDirection.BUY
                    && entity.getOrderType() == OrderType.LIMIT
                    && entity.getVolume() == 2
                    && entity.getStatus() == OrderStatus.PENDING;
        }));
    }

    @Test
    void testPlaceMarketOrder_Success() {
        when(orderMapper.insert(any())).thenAnswer(inv -> {
            ((OrderEntity) inv.getArgument(0)).setId(2L);
            return 1;
        });

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("GC");
        req.setDirection("SELL");
        req.setOrderType("MARKET");
        req.setVolume(1);

        OrderVO result = orderService.placeOrder(req);
        assertNotNull(result);
        assertEquals("MARKET", result.getOrderType());
        assertEquals("SELL", result.getDirection());
    }

    @Test
    void testPlaceStopOrder_Success() {
        when(orderMapper.insert(any())).thenAnswer(inv -> {
            ((OrderEntity) inv.getArgument(0)).setId(3L);
            return 1;
        });

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("CL");
        req.setDirection("BUY");
        req.setOrderType("STOP");
        req.setStopPrice(BigDecimal.valueOf(75.50));
        req.setVolume(3);

        OrderVO result = orderService.placeOrder(req);
        assertNotNull(result);
        assertEquals("STOP", result.getOrderType());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void testPlaceOrder_InvalidVolume_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(4500));
        req.setVolume(0);

        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void testPlaceOrder_VolumeTooLarge_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(4500));
        req.setVolume(10000);

        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void testPlaceOrder_InvalidSymbol_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("INVALID");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(100));
        req.setVolume(1);

        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void testPlaceOrder_LimitPriceZero_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.ZERO);
        req.setVolume(1);

        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void testPlaceOrder_StopPriceMissing_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("STOP");
        req.setVolume(1);

        assertThrows(BizException.class, () -> orderService.placeOrder(req));
    }

    // ==================== 撤单测试 ====================

    @Test
    void testCancelOrder_Success() {
        Long orderId = 1L;
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setUserId(1L);
        order.setOrderId("ORD123");
        order.setStatus(OrderStatus.PENDING);
        order.setVolume(5);
        order.setFilledVolume(0);

        when(orderMapper.selectById(orderId)).thenReturn(order);

        orderService.cancelOrder(orderId, 1L);

        verify(orderMapper).updateById(argThat(e ->
                e.getStatus() == OrderStatus.CANCELLED));
    }

    @Test
    void testCancelOrder_WrongUser_ShouldThrow() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING);

        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BizException.class,
                () -> orderService.cancelOrder(1L, 2L));
    }

    @Test
    void testCancelOrder_AlreadyFilled_ShouldThrow() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.FILLED);

        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BizException.class,
                () -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    void testCancelOrder_NotFound_ShouldThrow() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class,
                () -> orderService.cancelOrder(999L, 1L));
    }

    // ==================== 成交回报测试 ====================

    @Test
    void testHandleMatchEvent_PartialFill() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId("ORD123");
        order.setVolume(10);
        order.setFilledVolume(0);
        order.setStatus(OrderStatus.PENDING);

        when(orderMapper.selectOne(any())).thenReturn(order);

        OrderMatchEvent event = OrderMatchEvent.builder()
                .orderId("ORD123")
                .matchVolume(3)
                .matchPrice(BigDecimal.valueOf(4500))
                .totalFilledVolume(3)
                .avgPrice(BigDecimal.valueOf(4500))
                .newStatus("PARTIAL")
                .matchedAt(LocalDateTime.now())
                .build();

        orderService.handleMatchEvent(event);

        verify(orderMapper).updateById(argThat(e ->
                e.getFilledVolume() == 3 &&
                e.getStatus() == OrderStatus.PARTIAL));
    }

    @Test
    void testHandleMatchEvent_FullFill() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderId("ORD123");
        order.setVolume(2);
        order.setFilledVolume(0);
        order.setStatus(OrderStatus.PENDING);

        when(orderMapper.selectOne(any())).thenReturn(order);

        OrderMatchEvent event = OrderMatchEvent.builder()
                .orderId("ORD123")
                .matchVolume(2)
                .matchPrice(BigDecimal.valueOf(4500))
                .totalFilledVolume(2)
                .avgPrice(BigDecimal.valueOf(4500))
                .newStatus("FILLED")
                .matchedAt(LocalDateTime.now())
                .build();

        orderService.handleMatchEvent(event);

        verify(orderMapper).updateById(argThat(e ->
                e.getFilledVolume() == 2 &&
                e.getStatus() == OrderStatus.FILLED));
    }

    // ==================== 括号单测试 ====================

    @Test
    void testPlaceBracketOrder_Success() {
        when(orderMapper.insert(any())).thenAnswer(inv -> {
            long id = (long) (Math.random() * 1000);
            ((OrderEntity) inv.getArgument(0)).setId(id);
            return 1;
        });
        doNothing().when(orderMapper).updateParentIdById(anyLong(), anyLong());

        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(4500));
        req.setVolume(2);
        req.setTakeProfitPrice(BigDecimal.valueOf(4550));
        req.setStopPrice(BigDecimal.valueOf(4450));

        var result = orderService.placeBracketOrder(req);
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals("BUY", result.get(0).getDirection()); // parent
        assertEquals("SELL", result.get(1).getDirection()); // tp
        assertEquals("SELL", result.get(2).getDirection()); // sl
    }

    @Test
    void testPlaceBracketOrder_InvalidPrices_ShouldThrow() {
        OrderPlaceRequest req = new OrderPlaceRequest();
        req.setUserId(1L);
        req.setSymbol("ES");
        req.setDirection("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(BigDecimal.valueOf(4500));
        req.setVolume(2);
        req.setTakeProfitPrice(BigDecimal.valueOf(4400));
        req.setStopPrice(BigDecimal.valueOf(4500));

        assertThrows(BizException.class,
                () -> orderService.placeBracketOrder(req));
    }

    // ==================== 查询测试 ====================

    @Test
    void testGetOrderDetail_Success() {
        OrderEntity entity = new OrderEntity();
        entity.setId(1L);
        entity.setOrderId("ORD123");
        entity.setUserId(1L);
        entity.setSymbol("ES");
        entity.setDirection(OrderDirection.BUY);
        entity.setOrderType(OrderType.LIMIT);
       entity.setPrice(BigDecimal.valueOf(4500));
       entity.setVolume(2);
       entity.setStatus(OrderStatus.PENDING);
        entity.setFilledVolume(0);

        when(orderMapper.selectById(1L)).thenReturn(entity);

        OrderVO detail = orderService.getOrderDetail(1L);
        assertNotNull(detail);
        assertEquals("ORD123", detail.getOrderId());
    }

    @Test
    void testGetOrderDetail_NotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        assertThrows(BizException.class,
                () -> orderService.getOrderDetail(999L));
    }
}
