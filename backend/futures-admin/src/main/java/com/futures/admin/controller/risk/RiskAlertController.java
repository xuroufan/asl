package com.futures.admin.controller.risk;

import com.futures.admin.client.RiskApiService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 异常交易报警控制器。
 * <p>管理和处理风控预警、异常交易报警。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/risk/alert")
@RequiredArgsConstructor
public class RiskAlertController {

    private final RiskApiService riskApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('risk:alert:list')")
    public Result<?> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String alertType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return riskApiService.getRiskAlerts(userId, alertType, page, size);
    }
}
