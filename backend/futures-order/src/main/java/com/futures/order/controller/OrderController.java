package com.futures.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import com.futures.order.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import com.futures.order.dto.OrderCancelRequest;
import com.futures.order.dto.OrderPlaceRequest;
import com.futures.order.dto.OrderVO;
import com.futures.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenUtil jwtTokenUtil;

    /** 从 header 提取 userId，优先 X-User-Id，其次 JWT */
    private Long resolveUserId(Long headerUserId, String authHeader) {
        if (headerUserId != null) return headerUserId;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7)); } catch (Exception e) { return null; }
        }
        return null;
    }

    @PostMapping("/place")
    public Result<?> placeOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody OrderPlaceRequest request) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        request.setUserId(userId);

        if (request.getTakeProfitPrice() != null && request.getStopPrice() != null) {
            List<OrderVO> orders = orderService.placeBracketOrder(request);
            return Result.success(orders);
        }
        OrderVO order = orderService.placeOrder(request);
        return Result.success(order);
    }

    @PostMapping("/bracket")
    public Result<List<OrderVO>> placeBracket(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody OrderPlaceRequest request) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        request.setUserId(userId);
        return Result.success(orderService.placeBracketOrder(request));
    }

    @PostMapping("/cancel")
    public Result<Void> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody OrderCancelRequest request) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        orderService.cancelOrder(request.getOrderId(), userId);
        return Result.success();
    }

    @GetMapping("/current")
    public Result<List<OrderVO>> currentOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String symbol) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        return Result.success(orderService.getCurrentOrders(userId, symbol));
    }

    @GetMapping("/history")
    public Result<PageResult<OrderVO>> orderHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        Page<OrderVO> p = orderService.getOrderHistory(userId, symbol, startDate, endDate, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> orderDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long orderId) {
        Long userId = resolveUserId(headerUserId, authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        return Result.success(orderService.getOrderDetail(orderId));
    }
}
