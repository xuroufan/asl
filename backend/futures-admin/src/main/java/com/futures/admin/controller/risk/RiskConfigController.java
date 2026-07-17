package com.futures.admin.controller.risk;

import com.futures.admin.client.RiskApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 风控规则配置控制器。
 * <p>管理保证金率、持仓限额、强平阈值等风控参数。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/risk/config")
@RequiredArgsConstructor
public class RiskConfigController {

    private final RiskApiService riskApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('risk:config:list')")
    public Result<?> list() {
        Result<List<Map<String, Object>>> result = riskApiService.listRiskConfigs();
        return result != null ? result : Result.error("风控服务不可用");
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('risk:config:update')")
    public Result<?> update(@RequestBody Map<String, Object> config) {
        return riskApiService.updateRiskConfig(config);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('risk:config:update')")
    public Result<?> refresh() {
        return riskApiService.refreshConfig();
    }
}
