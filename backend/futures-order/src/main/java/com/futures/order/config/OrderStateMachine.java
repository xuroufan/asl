package com.futures.order.config;

import com.futures.common.exception.BizException;
import com.futures.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机
 * <p>
 * 定义合法的状态转移路径：
 * <pre>
 *   PENDING  ──匹配中──→ PARTIAL  ──匹配中──→ FILLED
 *   PENDING  ──撤单────→ CANCELLED
 *   PENDING  ──拒绝────→ REJECTED
 *   PARTIAL  ──匹配中──→ FILLED
 *   PARTIAL  ──撤单────→ CANCELLED
 * </pre>
 * 终止态 (FILLED / CANCELLED / REJECTED) 不允许再次转移。
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING, Set.of(
                OrderStatus.PARTIAL, OrderStatus.FILLED,
                OrderStatus.CANCELLED, OrderStatus.REJECTED));
        TRANSITIONS.put(OrderStatus.PARTIAL, Set.of(
                OrderStatus.FILLED, OrderStatus.CANCELLED));
        // 终止态不可转移
        TRANSITIONS.put(OrderStatus.FILLED, new HashSet<>());
        TRANSITIONS.put(OrderStatus.CANCELLED, new HashSet<>());
        TRANSITIONS.put(OrderStatus.REJECTED, new HashSet<>());
    }

    /**
     * 尝试从当前状态转移到目标状态
     *
     * @param current 当前状态
     * @param target  目标状态
     * @return true 转移合法
     * @throws BizException 如果转移非法
     */
    public boolean transition(OrderStatus current, OrderStatus target) {
        Set<OrderStatus> allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw BizException.badRequest(
                    String.format("非法状态转移: %s -> %s", current, target));
        }
        return true;
    }

    /** 判断当前状态是否可取消 */
    public boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.PARTIAL;
    }

    /** 判断当前是否活跃（已报或部成） */
    public boolean isActive(OrderStatus status) {
        return status.isActive();
    }
}
