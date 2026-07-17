package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 告警管理控制器。
 * <p>接收 Prometheus AlertManager 告警、认领处理、统计等。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/alert")
@RequiredArgsConstructor
public class OpsAlertController {

    private final OpsApiService opsApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ops:alert:list')")
    public Result<?> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return opsApiService.getAlerts(level, status, serviceName, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ops:alert:list')")
    public Result<?> stats() {
        return opsApiService.getAlertStats();
    }

    @PostMapping("/claim")
    @PreAuthorize("hasAuthority('ops:alert:handle')")
    public Result<?> claim(@RequestParam String alertId) {
        return opsApiService.claimAlert(alertId);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('ops:alert:handle')")
    public Result<?> resolve(
            @RequestParam String alertId,
            @RequestParam String resolution,
            @RequestParam(required = false) String notes) {
        return opsApiService.resolveAlert(alertId, resolution, notes);
    }
}
