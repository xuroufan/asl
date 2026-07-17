package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 配置变更管理控制器。
 * <p>管理 Nacos 配置的查看、变更申请、审批、对比等。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/config")
@RequiredArgsConstructor
public class OpsConfigController {

    private final OpsApiService opsApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ops:config:list')")
    public Result<?> list(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "dev") String env) {
        return opsApiService.getNacosConfigs(serviceName, env);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('ops:config:list')")
    public Result<?> detail(@RequestParam String configId) {
        return opsApiService.getConfigDetail(configId);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ops:config:list')")
    public Result<?> history(@RequestParam String configId) {
        return opsApiService.getConfigChangeHistory(configId);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('ops:config:apply')")
    public Result<?> apply(@RequestBody Map<String, Object> change) {
        return opsApiService.applyConfigChange(change);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('ops:config:approve')")
    public Result<?> approve(
            @RequestParam String changeId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment) {
        return opsApiService.approveConfigChange(changeId, approved, comment);
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('ops:config:list')")
    public Result<?> compare(
            @RequestParam String serviceName,
            @RequestParam String envA,
            @RequestParam String envB) {
        return opsApiService.compareConfigs(serviceName, envA, envB);
    }
}
