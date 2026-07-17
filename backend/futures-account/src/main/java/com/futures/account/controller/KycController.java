package com.futures.account.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.account.dto.KycSubmitRequest;
import com.futures.account.entity.KycRecordEntity;
import com.futures.account.service.KycService;
import com.futures.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * KYC（实名认证）控制器。
 * <p>提供用户提交 KYC、查询状态，以及管理员审核的接口。</p>
 */
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    /**
     * 用户提交 KYC 认证材料。
     */
    @PostMapping("/submit")
    public Result<String> submitKyc(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody KycSubmitRequest request) {
        kycService.submitKyc(userId, request);
        return Result.success("KYC 申请已提交，请等待审核");
    }

    /**
     * 查询当前用户的 KYC 状态。
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getKycStatus(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(kycService.getKycStatus(userId));
    }

    /**
     * 管理员 — 查询待审核 KYC 列表。
     */
    @GetMapping("/pending")
    public Result<IPage<KycRecordEntity>> getPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(kycService.getPendingList(page, size));
    }

    /**
     * 管理员 — 审核通过 KYC。
     */
    @PostMapping("/{recordId}/approve")
    public Result<String> approveKyc(
            @PathVariable Long recordId,
            @RequestHeader("X-User-Id") String reviewer) {
        kycService.approveKyc(recordId, reviewer);
        return Result.success("KYC 审核通过");
    }

    /**
     * 管理员 — 拒绝 KYC。
     */
    @PostMapping("/{recordId}/reject")
    public Result<String> rejectKyc(
            @PathVariable Long recordId,
            @RequestHeader("X-User-Id") String reviewer,
            @RequestBody Map<String, String> body) {
        kycService.rejectKyc(recordId, reviewer, body.get("remark"));
        return Result.success("KYC 已拒绝");
    }
}
