package com.futures.admin.controller.risk;

import com.futures.admin.client.RiskApiService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 强平记录管理控制器。
 * <p>查询和管理强制平仓记录。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/risk/liquidation")
@RequiredArgsConstructor
public class RiskLiquidationController {

    private final RiskApiService riskApiService;

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('risk:liquidation:list')")
    public Result<?> records(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return riskApiService.getLiquidationRecords(userId, page, size);
    }
}
