package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务状态监控控制器。
 * <p>展示所有微服务运行状态、实例健康、依赖拓扑等。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/service")
@RequiredArgsConstructor
public class OpsServiceController {

    private final OpsApiService opsApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ops:service:list')")
    public Result<?> list() {
        return opsApiService.getServices();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ops:service:list')")
    public Result<?> dashboard() {
        Map<String, Object> data = opsApiService.getServiceDashboard();
        return Result.success(data);
    }

    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('ops:service:list')")
    public Result<?> instances(@RequestParam String serviceName) {
        return opsApiService.getServiceInstances(serviceName);
    }
}
