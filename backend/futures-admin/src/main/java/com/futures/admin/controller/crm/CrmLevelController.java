package com.futures.admin.controller.crm;

import com.futures.admin.client.CrmApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户等级管理控制器。
 * <p>等级定义、升级规则、等级变更历史。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/crm/level")
@RequiredArgsConstructor
public class CrmLevelController {

    private final CrmApiService crmApiService;

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('crm:level:list')")
    public Result<?> definitions() {
        return crmApiService.getLevelDefinitions();
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('crm:level:list')")
    public Result<?> rules() {
        return crmApiService.getLevelRules();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('crm:level:list')")
    public Result<?> history(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return crmApiService.getLevelHistory(customerId, page, size);
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('crm:level:update')")
    public Result<?> update(
            @RequestParam Long customerId,
            @RequestParam String newLevel,
            @RequestParam(defaultValue = "") String reason) {
        return crmApiService.updateCustomerLevel(customerId, newLevel, reason);
    }
}
