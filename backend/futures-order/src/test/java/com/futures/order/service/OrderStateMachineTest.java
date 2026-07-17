package com.futures.order.service;

import com.futures.common.exception.BizException;
import com.futures.order.config.OrderStateMachine;
import com.futures.order.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单状态机单元测试
 */
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    void testPendingToPartial() {
        assertTrue(stateMachine.transition(OrderStatus.PENDING, OrderStatus.PARTIAL));
    }

    @Test
    void testPendingToFilled() {
        assertTrue(stateMachine.transition(OrderStatus.PENDING, OrderStatus.FILLED));
    }

    @Test
    void testPendingToCancelled() {
        assertTrue(stateMachine.transition(OrderStatus.PENDING, OrderStatus.CANCELLED));
    }

    @Test
    void testPendingToRejected() {
        assertTrue(stateMachine.transition(OrderStatus.PENDING, OrderStatus.REJECTED));
    }

    @Test
    void testPartialToFilled() {
        assertTrue(stateMachine.transition(OrderStatus.PARTIAL, OrderStatus.FILLED));
    }

    @Test
    void testPartialToCancelled() {
        assertTrue(stateMachine.transition(OrderStatus.PARTIAL, OrderStatus.CANCELLED));
    }

    @Test
    void testFilledCannotTransition() {
        assertThrows(BizException.class,
                () -> stateMachine.transition(OrderStatus.FILLED, OrderStatus.CANCELLED));
    }

    @Test
    void testCancelledCannotTransition() {
        assertThrows(BizException.class,
                () -> stateMachine.transition(OrderStatus.CANCELLED, OrderStatus.PENDING));
    }

    @Test
    void testRejectedCannotTransition() {
        assertThrows(BizException.class,
                () -> stateMachine.transition(OrderStatus.REJECTED, OrderStatus.PENDING));
    }

    @Test
    void testCanCancelPending() {
        assertTrue(stateMachine.canCancel(OrderStatus.PENDING));
    }

    @Test
    void testCanCancelPartial() {
        assertTrue(stateMachine.canCancel(OrderStatus.PARTIAL));
    }

    @Test
    void testCannotCancelFilled() {
        assertFalse(stateMachine.canCancel(OrderStatus.FILLED));
    }

    @Test
    void testIsActive() {
        assertTrue(stateMachine.isActive(OrderStatus.PENDING));
        assertTrue(stateMachine.isActive(OrderStatus.PARTIAL));
        assertFalse(stateMachine.isActive(OrderStatus.FILLED));
        assertFalse(stateMachine.isActive(OrderStatus.CANCELLED));
        assertFalse(stateMachine.isActive(OrderStatus.REJECTED));
    }
}
