package com.futures.admin.controller.finance;

import com.futures.admin.client.SettlementApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 财务统计看板控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/finance/dashboard")
@RequiredArgsConstructor
public class FinanceDashboardController {

    private final SettlementApiService settlementApiService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:dashboard:list')")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = settlementApiService.getFinanceDashboardData();
        return Result.success(data);
    }
}
