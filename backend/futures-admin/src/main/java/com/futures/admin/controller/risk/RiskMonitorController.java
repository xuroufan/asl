package com.futures.admin.controller.risk;

import com.futures.admin.client.RiskApiService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 风控监控大屏控制器。
 * <p>提供实时风控仪表盘数据。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/risk/monitor")
@RequiredArgsConstructor
public class RiskMonitorController {

    private final RiskApiService riskApiService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('risk:monitor:list')")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = riskApiService.getRiskDashboardData();
        return Result.success(data);
    }

    @GetMapping("/user-status")
    @PreAuthorize("hasAuthority('risk:monitor:list')")
    public Result<?> userStatus(@RequestParam Long userId) {
        return riskApiService.getUserRiskStatus(userId);
    }
}
