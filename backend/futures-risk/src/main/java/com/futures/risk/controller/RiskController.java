package com.futures.risk.controller;

import com.futures.common.result.Result;
import com.futures.risk.entity.RiskAlertEntity;
import com.futures.risk.entity.RiskConfigEntity;
import com.futures.risk.service.LiquidationService;
import com.futures.risk.service.RiskCheckService;
import com.futures.risk.service.RiskConfigService;
import com.futures.risk.service.RiskMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风控引擎 REST 接口
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Tag(name = "风控引擎", description = "风险管理相关接口")
public class RiskController {

    private final RiskCheckService riskCheckService;
    private final RiskMonitorService riskMonitorService;
    private final RiskConfigService riskConfigService;
    private final LiquidationService liquidationService;

    @PostMapping("/check")
    @Operation(summary = "前置风控校验")
    public Result<RiskCheckService.RiskCheckResult> preCheck(
            @RequestBody RiskCheckService.PreCheckRequest request) {
        return Result.success(riskCheckService.preCheck(request));
    }

    @GetMapping("/status")
    @Operation(summary = "查询用户风险状态")
    public Result<RiskMonitorService.RiskStatus> getRiskStatus(
            @RequestParam String userId) {
        return Result.success(riskMonitorService.calculateRisk(userId));
    }

    @GetMapping("/config")
    @Operation(summary = "查询所有风控配置")
    public Result<List<RiskConfigEntity>> getAllConfig() {
        return Result.success(riskConfigService.getAllConfigs());
    }

    @GetMapping("/config/{symbol}")
    @Operation(summary = "查询品种风控配置")
    public Result<RiskConfigEntity> getConfig(@PathVariable String symbol) {
        return Result.success(riskConfigService.getConfig(symbol));
    }

    @PostMapping("/config")
    @Operation(summary = "更新风控配置")
    public Result<String> updateConfig(@RequestBody RiskConfigEntity config) {
        riskConfigService.updateConfig(config);
        return Result.success("配置更新成功");
    }

    @GetMapping("/alert/list")
    @Operation(summary = "查询预警列表")
    public Result<List<RiskAlertEntity>> getAlerts() {
        // TODO: 支持分页
        return Result.success(java.util.Collections.emptyList());
    }
}
