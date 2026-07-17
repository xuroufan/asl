package com.futures.admin.controller.crm;

import com.futures.admin.client.CrmApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户信息管理控制器。
 * <p>客户档案、全方位客户视图、客户搜索。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/crm/customer")
@RequiredArgsConstructor
public class CrmCustomerController {

    private final CrmApiService crmApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return crmApiService.getCustomers(keyword, level, kycStatus, status, page, size);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<?> detail(@RequestParam Long customerId) {
        return crmApiService.getCustomerDetail(customerId);
    }

    @GetMapping("/portfolio")
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<?> portfolio(@RequestParam Long customerId) {
        return crmApiService.getCustomerPortfolio(customerId);
    }
}
