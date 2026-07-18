package com.futures.order.controller;

import com.futures.common.result.Result;
import com.futures.order.dto.ClosePositionRequest;
import com.futures.order.dto.PositionVO;
import com.futures.order.security.JwtTokenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/position")
@RequiredArgsConstructor
public class PositionController {

    private final JwtTokenUtil jwtTokenUtil;

    private Long resolveUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7));
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    @GetMapping("/list")
    public Result<List<PositionVO>> getPositions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        // 返回模拟持仓数据（与TradeController一致）
        List<PositionVO> positions = new ArrayList<>();
        positions.add(PositionVO.builder()
                .symbol("ES").direction("BUY").volume(2)
                .avgPrice(new BigDecimal("4480.50"))
                .currentPrice(new BigDecimal("4500.00"))
                .unrealizedPnl(new BigDecimal("3900.00"))
                .margin(new BigDecimal("896.10"))
                .profitRate(new BigDecimal("0.44"))
                .build());
        positions.add(PositionVO.builder()
                .symbol("GC").direction("SELL").volume(1)
                .avgPrice(new BigDecimal("1995.00"))
                .currentPrice(new BigDecimal("1980.50"))
                .unrealizedPnl(new BigDecimal("1450.00"))
                .margin(new BigDecimal("199.50"))
                .profitRate(new BigDecimal("0.73"))
                .build());
        return Result.success(positions);
    }

    @PostMapping("/close")
    public Result<Void> closePosition(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ClosePositionRequest request) {
        Long userId = resolveUserId(authHeader);
        if (userId == null) return Result.error(400, "无法识别用户身份");
        log.info("平仓: userId={}, symbol={}, volume={}", userId, request.getSymbol(), request.getVolume());
        return Result.success();
    }
}
