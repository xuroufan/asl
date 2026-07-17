package com.futures.admin.controller.crm;

import com.futures.admin.client.CrmApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 客户标签管理控制器。
 * <p>自定义标签创建、打标签/去标签、按标签筛选客户。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/crm/tag")
@RequiredArgsConstructor
public class CrmTagController {

    private final CrmApiService crmApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('crm:tag:list')")
    public Result<?> list(@RequestParam(required = false) String category) {
        return crmApiService.getTags(category);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('crm:tag:create')")
    public Result<?> create(@RequestBody Map<String, Object> tag) {
        return crmApiService.createTag(tag);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('crm:tag:delete')")
    public Result<?> delete(@RequestParam Long tagId) {
        return crmApiService.deleteTag(tagId);
    }

    @GetMapping("/customer-tags")
    @PreAuthorize("hasAuthority('crm:tag:list')")
    public Result<?> customerTags(@RequestParam Long customerId) {
        return crmApiService.getCustomerTags(customerId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('crm:tag:assign')")
    public Result<?> assign(@RequestParam Long customerId, @RequestParam Long tagId) {
        return crmApiService.assignTagToCustomer(customerId, tagId);
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAuthority('crm:tag:assign')")
    public Result<?> remove(@RequestParam Long customerId, @RequestParam Long tagId) {
        return crmApiService.removeTagFromCustomer(customerId, tagId);
    }
}
