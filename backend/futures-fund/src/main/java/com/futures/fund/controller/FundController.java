package com.futures.fund.controller;

import com.futures.common.result.Result;
import com.futures.fund.entity.FundAccountEntity;
import com.futures.fund.service.FundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 资金管理 REST 接口
 */
@RestController
@RequestMapping("/api/v1/fund")
@RequiredArgsConstructor
@Tag(name = "资金管理", description = "资金账户操作接口")
public class FundController {

    private final FundService fundService;

    @PostMapping("/freeze")
    @Operation(summary = "冻结保证金")
    public Result<FreezeResponse> freeze(@RequestBody @Valid FreezeRequest request) {
        boolean success = fundService.freeze(
                request.getUserId(), request.getAmount(), request.getOrderId());

        FundAccountEntity account = fundService.getBalanceOverview(request.getUserId());

        FreezeResponse resp = new FreezeResponse();
        resp.setSuccess(success);
        resp.setAvailableAfter(account.getAvailable());
        resp.setFrozenAfter(account.getFrozen());
        resp.setMessage(success ? "冻结成功" : "冻结失败");
        return Result.success(resp);
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻保证金")
    public Result<FreezeResponse> unfreeze(@RequestBody @Valid FreezeRequest request) {
        boolean success = fundService.unfreeze(
                request.getUserId(), request.getAmount(), request.getOrderId());

        FundAccountEntity account = fundService.getBalanceOverview(request.getUserId());

        FreezeResponse resp = new FreezeResponse();
        resp.setSuccess(success);
        resp.setAvailableAfter(account.getAvailable());
        resp.setFrozenAfter(account.getFrozen());
        resp.setMessage(success ? "解冻成功" : "解冻失败");
        return Result.success(resp);
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减资金")
    public Result<FreezeResponse> deduct(@RequestBody @Valid FreezeRequest request) {
        boolean success = fundService.deduct(
                request.getUserId(), request.getAmount(), request.getOrderId());

        FundAccountEntity account = fundService.getBalanceOverview(request.getUserId());

        FreezeResponse resp = new FreezeResponse();
        resp.setSuccess(success);
        resp.setAvailableAfter(account.getAvailable());
        resp.setFrozenAfter(account.getFrozen());
        resp.setMessage(success ? "扣款成功" : "扣款失败");
        return Result.success(resp);
    }

    @PostMapping("/deposit")
    @Operation(summary = "入金")
    public Result<String> deposit(@RequestBody @Valid DepositRequest request) {
        fundService.deposit(request.getUserId(), request.getAmount());
        return Result.success("入金成功");
    }

    @GetMapping("/balance")
    @Operation(summary = "查询资金余额")
    public Result<FundAccountEntity> getBalance(@RequestParam String userId) {
        return Result.success(fundService.getBalanceOverview(userId));
    }

    @GetMapping("/available")
    @Operation(summary = "查询可用资金")
    public Result<BigDecimal> getAvailable(@RequestParam String userId) {
        return Result.success(fundService.getAvailable(userId));
    }

    // ==================== DTO ====================

    @Data
    public static class FreezeRequest {
        @NotBlank private String userId;
        @DecimalMin("0.01") private BigDecimal amount;
        private String orderId;
    }

    @Data
    public static class FreezeResponse {
        private boolean success;
        private BigDecimal availableAfter;
        private BigDecimal frozenAfter;
        private String message;
    }

    @Data
    public static class DepositRequest {
        @NotBlank private String userId;
        @DecimalMin("0.01") private BigDecimal amount;
    }
}
