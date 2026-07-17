package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日志查询平台控制器。
 * <p>集成 ELK 的日志查询入口，支持多维度筛选和 traceId 全链路查询。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/log")
@RequiredArgsConstructor
public class OpsLogController {

    private final OpsApiService opsApiService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ops:log:search')")
    public Result<?> search(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return opsApiService.searchLogs(serviceName, level, keyword, traceId, startTime, endTime, page, size);
    }

    @GetMapping("/context")
    @PreAuthorize("hasAuthority('ops:log:search')")
    public Result<?> context(@RequestParam String traceId, @RequestParam(required = false) String timestamp) {
        return Result.success(opsApiService.getLogContext(traceId, timestamp));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ops:log:search')")
    public Result<?> stats(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(opsApiService.getLogStats(serviceName, startTime, endTime));
    }
}
