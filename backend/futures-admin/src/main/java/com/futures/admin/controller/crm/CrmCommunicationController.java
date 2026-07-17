package com.futures.admin.controller.crm;

import com.futures.admin.client.CrmApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 客户沟通记录控制器。
 * <p>记录沟通历史、跟进提醒。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/crm/communication")
@RequiredArgsConstructor
public class CrmCommunicationController {

    private final CrmApiService crmApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('crm:communication:list')")
    public Result<?> list(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return crmApiService.getCommunications(customerId, page, size);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('crm:communication:create')")
    public Result<?> create(@RequestBody Map<String, Object> comm) {
        return crmApiService.createCommunication(comm);
    }

    @GetMapping("/follow-ups")
    @PreAuthorize("hasAuthority('crm:communication:list')")
    public Result<?> followUps(
            @RequestParam(required = false) String staffName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return crmApiService.getFollowUpReminders(staffName, page, size);
    }
}
