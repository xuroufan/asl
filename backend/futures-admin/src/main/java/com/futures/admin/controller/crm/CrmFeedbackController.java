package com.futures.admin.controller.crm;

import com.futures.admin.client.CrmApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 客户反馈处理控制器。
 * <p>反馈收集、分配、状态流转、统计分析。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/crm/feedback")
@RequiredArgsConstructor
public class CrmFeedbackController {

    private final CrmApiService crmApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('crm:feedback:list')")
    public Result<?> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return crmApiService.getFeedbacks(status, type, assignee, page, size);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('crm:feedback:create')")
    public Result<?> create(@RequestBody Map<String, Object> feedback) {
        return crmApiService.createFeedback(feedback);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('crm:feedback:assign')")
    public Result<?> assign(@RequestParam Long feedbackId, @RequestParam String assignee) {
        return crmApiService.assignFeedback(feedbackId, assignee);
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('crm:feedback:handle')")
    public Result<?> status(
            @RequestParam Long feedbackId,
            @RequestParam String status,
            @RequestParam(required = false) String resolution) {
        return crmApiService.updateFeedbackStatus(feedbackId, status, resolution);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('crm:feedback:list')")
    public Result<?> stats() {
        return crmApiService.getFeedbackStats();
    }
}
