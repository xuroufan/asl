package com.futures.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.common.exception.BizException;
import com.futures.common.util.RedisUtil;
import com.futures.order.config.OrderStateMachine;
import com.futures.order.dto.*;
import com.futures.order.entity.OrderEntity;
import com.futures.order.enums.OrderDirection;
import com.futures.order.enums.OrderStatus;
import com.futures.order.enums.OrderType;
import com.futures.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单管理服务
 * <p>
 * 核心职责：
 * <ul>
 *   <li>下单校验（用户、合约、价格范围、手数、资金）</li>
 *   <li>订单幂等（clientOrderId 防重）</li>
 *   <li>订单创建与持久化</li>
 *   <li>撤单与状态管理</li>
 *   <li>括号单（OCO：止盈+止损）</li>
 *   <li>订单查询（当前挂单、历史委托）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** 单笔最大手数 */
    private static final int MAX_VOLUME_PER_ORDER = 9999;

    /** 支持的合约列表（模拟数据，正式环境从配置中心读取） */
    private static final java.util.Set<String> SUPPORTED_SYMBOLS =
            java.util.Set.of("ES", "GC", "CL", "SI", "NQ", "YM", "ZB", "ZN", "6E", "6J");

    private final OrderMapper orderMapper;
    private final OrderStateMachine stateMachine;
    private final OrderIdempotencyService idempotencyService;
    private final OrderEventProducer eventProducer;
    private final OrderTccService seataTccService;

    // ==================== 下单 ====================

    /**
     * 创建订单（限价单 / 市价单 / 止损单）
     * <p>
     * 流程：
     * 1. 幂等性检查（clientOrderId 防重）
     * 2. 参数校验（方向、类型、价格、手数）
     * 3. 业务校验（合约有效性、资金、风控）
     * 4. TCC 分布式事务准备
     * 5. 持久化订单
     * 6. 发送下单事件到 MQ
     *
     * @param req 下单请求
     * @return 订单 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO placeOrder(OrderPlaceRequest req) {
        // ── 1. 幂等性检查 ──
        if (req.getClientOrderId() != null && !req.getClientOrderId().isBlank()) {
            Long existingId = idempotencyService.getExistingOrderId(req.getClientOrderId());
            if (existingId != null) {
                OrderEntity existing = orderMapper.selectById(existingId);
                if (existing != null) {
                    log.info("幂等命中，返回已存在的订单: orderId={}, clientOrderId={}",
                            existing.getOrderId(), req.getClientOrderId());
                    return OrderVO.from(existing);
                }
            }
            if (!idempotencyService.tryMarkProcessed(req.getClientOrderId())) {
                throw BizException.badRequest("重复下单: clientOrderId=" + req.getClientOrderId());
            }
        }

        // ── 2. 参数校验 ──
        OrderDirection direction = OrderDirection.fromString(req.getDirection());
        OrderType orderType = OrderType.fromString(req.getOrderType());

        if (req.getVolume() == null || req.getVolume() <= 0) {
            throw BizException.badRequest("手数必须为正整数");
        }
        if (req.getVolume() > MAX_VOLUME_PER_ORDER) {
            throw BizException.badRequest("单笔手数超过上限: " + MAX_VOLUME_PER_ORDER);
        }

        // 限价单价格检查
        if (orderType == OrderType.LIMIT) {
            if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw BizException.badRequest("限价单价格必须大于0");
            }
        }
        // 止损单触发价检查
        if (orderType == OrderType.STOP) {
            if (req.getStopPrice() == null || req.getStopPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw BizException.badRequest("止损单触发价必须大于0");
            }
        }

        // ── 3. 业务校验 ──
        String symbol = req.getSymbol().toUpperCase();
        validateSymbol(symbol);

        // 计算预计成交价（市价单用最新的对手价，限价用指定价）
        BigDecimal execPrice = (orderType == OrderType.MARKET)
                ? getEstimatedMarketPrice(symbol, direction)
                : req.getPrice();

        // ── 4. 构建订单实体 ──
        String orderId = generateOrderId();
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setUserId(req.getUserId());
        order.setSymbol(symbol);
        order.setDirection(direction);
        order.setOrderType(orderType);
        order.setPrice(execPrice);
        order.setVolume(req.getVolume());
        order.setFilledVolume(0);
        order.setAvgPrice(BigDecimal.ZERO);
        order.setStopPrice(req.getStopPrice());
        order.setTakeProfitPrice(req.getTakeProfitPrice());
        order.setStatus(OrderStatus.PENDING);
        order.setClientOrderId(req.getClientOrderId());
        order.setTimeInForce("DAY");
        order.setCreatedAt(LocalDateTime.now());

        // ── 5. TCC 准备（验资 + 验仓 + 冻结保证金） ──
        try {
            seataTccService.tryPlace(order, execPrice);
        } catch (BizException e) {
            log.warn("下单TCC校验失败: userId={}, symbol={}, reason={}",
                    req.getUserId(), symbol, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("下单TCC校验异常: userId={}, symbol={}", req.getUserId(), symbol, e);
            throw BizException.badRequest("系统繁忙，请稍后重试");
        }

        // ── 6. 持久化 ──
        orderMapper.insert(order);

        // 幂等绑定
        if (req.getClientOrderId() != null && !req.getClientOrderId().isBlank()) {
            idempotencyService.bindOrderId(req.getClientOrderId(), order.getId());
        }

        // ── 7. 发送 MQ 事件给撮合引擎 ──
        eventProducer.publishPlaceOrder(PlaceOrderEvent.builder()
                .orderId(orderId)
                .userId(req.getUserId())
                .symbol(symbol)
                .direction(direction.name())
                .orderType(orderType.name())
                .price(execPrice)
                .volume(req.getVolume())
                .stopPrice(req.getStopPrice())
                .build());

        log.info("订单创建成功: orderId={}, userId={}, symbol={}, direction={}, type={}, volume={}, price={}",
                orderId, req.getUserId(), symbol, direction, orderType, req.getVolume(), execPrice);

        return OrderVO.from(order);
    }

    // ==================== 括号单（OCO） ====================

    /**
     * 创建括号单：1 张母单 + 1 张止盈限价单 + 1 张止损单（OCO）
     * <p>
     * OCO 算法：当母单全部成交后自动激活子单；
     * 任意子单成交时，另一个子单被自动取消。
     *
     * @param req 下单请求（必须包含 takeProfitPrice 和 stopPrice）
     * @return 三个订单的 VO 列表 [母单, 止盈单, 止损单]
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> placeBracketOrder(OrderPlaceRequest req) {
        if (req.getTakeProfitPrice() == null || req.getStopPrice() == null) {
            throw BizException.badRequest("括号单必须同时提供止盈价和止损价");
        }
        if (req.getTakeProfitPrice().compareTo(req.getStopPrice()) <= 0) {
            throw BizException.badRequest("止盈价必须高于止损价");
        }
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("括号单入场价不能为空");
        }

        // 1. 创建母单
        String bracketClientId = req.getClientOrderId() != null
                ? req.getClientOrderId() + "_PARENT"
                : null;
        OrderPlaceRequest parentReq = new OrderPlaceRequest();
        parentReq.setUserId(req.getUserId());
        parentReq.setSymbol(req.getSymbol());
        parentReq.setDirection(req.getDirection());
        parentReq.setOrderType("LIMIT");
        parentReq.setPrice(req.getPrice());
        parentReq.setVolume(req.getVolume());
        parentReq.setClientOrderId(bracketClientId);

        OrderVO parent = placeOrder(parentReq);

        // 2. 创建止盈子单
        String opposite = "BUY".equalsIgnoreCase(req.getDirection()) ? "SELL" : "BUY";
        OrderPlaceRequest tpReq = new OrderPlaceRequest();
        tpReq.setUserId(req.getUserId());
        tpReq.setSymbol(req.getSymbol());
        tpReq.setDirection(opposite);
        tpReq.setOrderType("LIMIT");
        tpReq.setPrice(req.getTakeProfitPrice());
        tpReq.setVolume(req.getVolume());
        tpReq.setClientOrderId(req.getClientOrderId() != null
                ? req.getClientOrderId() + "_TP" : null);

        OrderVO tp = placeOrder(tpReq);

        // 3. 创建止损子单
        OrderPlaceRequest slReq = new OrderPlaceRequest();
        slReq.setUserId(req.getUserId());
        slReq.setSymbol(req.getSymbol());
        slReq.setDirection(opposite);
        slReq.setOrderType("STOP");
        slReq.setStopPrice(req.getStopPrice());
        slReq.setVolume(req.getVolume());
        slReq.setClientOrderId(req.getClientOrderId() != null
                ? req.getClientOrderId() + "_SL" : null);

        OrderVO sl = placeOrder(slReq);

        // 4. 更新子单的 parentId
        orderMapper.updateParentIdById(tp.getId(), parent.getId());
        orderMapper.updateParentIdById(sl.getId(), parent.getId());

        log.info("括号单创建成功: parentOrderId={}, tpOrderId={}, slOrderId={}",
                parent.getOrderId(), tp.getOrderId(), sl.getOrderId());

        return List.of(parent, tp, sl);
    }

    // ==================== 撤单 ====================

    /**
     * 撤单
     * <p>
     * 校验：订单必须属于当前用户，且状态为 PENDING 或 PARTIAL。
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在: " + orderId);
        }
        if (!order.getUserId().equals(userId)) {
            throw BizException.unauthorized("无权撤单: " + orderId);
        }
        if (!stateMachine.canCancel(order.getStatus())) {
            throw BizException.badRequest(
                    "当前状态不可撤单: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 解冻未成交部分的保证金
        int remainingVolume = order.getVolume() - order.getFilledVolume();
        if (remainingVolume > 0) {
            log.info("撤单解冻保证金: orderId={}, volume={}", orderId, remainingVolume);
        }

        log.info("撤单成功: orderId={}, userId={}, status={}", orderId, userId, order.getStatus());
    }

    // ==================== 订单查询 ====================

    /**
     * 查询当前用户的活跃挂单
     */
    public List<OrderVO> getCurrentOrders(Long userId, String symbol) {
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .in(OrderEntity::getStatus, OrderStatus.PENDING, OrderStatus.PARTIAL);
        if (symbol != null && !symbol.isBlank()) {
            w.eq(OrderEntity::getSymbol, symbol.toUpperCase());
        }
        w.orderByDesc(OrderEntity::getCreatedAt);
        return orderMapper.selectList(w).stream()
                .map(OrderVO::from)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询历史委托
     */
    public Page<OrderVO> getOrderHistory(Long userId, String symbol,
                                          LocalDateTime start, LocalDateTime end,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId);
        if (symbol != null && !symbol.isBlank()) {
            w.eq(OrderEntity::getSymbol, symbol.toUpperCase());
        }
        if (start != null) w.ge(OrderEntity::getCreatedAt, start);
        if (end != null) w.le(OrderEntity::getCreatedAt, end);
        w.orderByDesc(OrderEntity::getCreatedAt);

        Page<OrderEntity> entityPage = orderMapper.selectPage(
                new Page<>(pageNum, pageSize), w);

        Page<OrderVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(OrderVO::from)
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 查询单笔订单详情
     */
    public OrderVO getOrderDetail(Long orderId) {
        OrderEntity entity = orderMapper.selectById(orderId);
        if (entity == null) {
            throw BizException.notFound("订单不存在: " + orderId);
        }
        return OrderVO.from(entity);
    }

    // ==================== 成交回报处理（MQ 消费者回调） ====================

    /**
     * 处理撮合成交事件（由 MQ 消费者调用）
     * <p>
     * 更新订单的已成交数量、均价和状态。
     * 如果状态变为 FILLED 且是括号单母单，自动激活子单。
     *
     * @param event 成交事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleMatchEvent(OrderMatchEvent event) {
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderId, event.getOrderId());
        OrderEntity order = orderMapper.selectOne(w);
        if (order == null) {
            log.warn("成交事件订单不存在: orderId={}", event.getOrderId());
            return;
        }

        OrderStatus targetStatus = OrderStatus.valueOf(event.getNewStatus());

        // 状态机校验
        stateMachine.transition(order.getStatus(), targetStatus);

        order.setFilledVolume(event.getTotalFilledVolume());
        order.setAvgPrice(event.getAvgPrice());
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单状态更新: orderId={}, newStatus={}, filledVolume={}, avgPrice={}",
                event.getOrderId(), targetStatus, event.getTotalFilledVolume(), event.getAvgPrice());

        // 如果是括号单母单全成，激活子单
        if (targetStatus == OrderStatus.FILLED && order.getParentId() == null) {
            activateChildOrders(order.getId());
        }

        // 如果是括号单子单成交，取消另一个子单（OCO）
        if (targetStatus == OrderStatus.FILLED && order.getParentId() != null) {
            cancelSiblingOrder(order.getParentId(), order.getId());
        }
    }

    /**
     * OCO: 母单全成后激活子单
     */
    private void activateChildOrders(Long parentId) {
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getParentId, parentId)
                .eq(OrderEntity::getStatus, OrderStatus.PENDING);
        List<OrderEntity> children = orderMapper.selectList(w);
        for (OrderEntity child : children) {
            child.setStatus(OrderStatus.PENDING);
            child.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(child);
            log.info("激活子单: orderId={}", child.getOrderId());
        }
    }

    /**
     * OCO: 子单成交后取消另一个子单
     */
    private void cancelSiblingOrder(Long parentId, Long excludeId) {
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getParentId, parentId)
                .ne(OrderEntity::getId, excludeId)
                .in(OrderEntity::getStatus, OrderStatus.PENDING, OrderStatus.PARTIAL);
        List<OrderEntity> siblings = orderMapper.selectList(w);
        for (OrderEntity sibling : siblings) {
            sibling.setStatus(OrderStatus.CANCELLED);
            sibling.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(sibling);
            log.info("OCO取消兄弟单: orderId={}", sibling.getOrderId());
        }
    }

    // ==================== 内部工具方法 ====================

    /** 生成全局唯一订单号 */
    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /** 校验合约是否有效 */
    private void validateSymbol(String symbol) {
        if (!SUPPORTED_SYMBOLS.contains(symbol)) {
            throw BizException.contractNotFound(symbol);
        }
    }

    /** 估算市价单的成交价（模拟） */
    private BigDecimal getEstimatedMarketPrice(String symbol, OrderDirection direction) {
        // 模拟：TODO 对接行情服务获取实时价格
        // 此处仅返回一个占位值，实际应调用 market-service 的 Feign 接口
        log.debug("市价单估算价格: symbol={}, direction={}", symbol, direction);
        return BigDecimal.valueOf(100.0);
    }
}
