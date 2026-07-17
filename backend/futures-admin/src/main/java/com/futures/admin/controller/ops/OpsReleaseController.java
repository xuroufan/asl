package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 服务发布管理控制器。
 * <p>管理发布单、灰度发布、回滚、发布历史等。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/release")
@RequiredArgsConstructor
public class OpsReleaseController {

    private final OpsApiService opsApiService;

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ops:release:list')")
    public Result<?> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return opsApiService.getReleaseHistory(page, size);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('ops:release:list')")
    public Result<?> detail(@RequestParam String releaseId) {
        return opsApiService.getReleaseDetail(releaseId);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ops:release:list')")
    public Result<?> stats() {
        return opsApiService.getReleaseStats();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ops:release:create')")
    public Result<?> create(@RequestBody Map<String, Object> release) {
        return opsApiService.createRelease(release);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('ops:release:approve')")
    public Result<?> approve(@RequestParam String releaseId, @RequestParam String comment) {
        return opsApiService.approveRelease(releaseId, comment);
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('ops:release:execute')")
    public Result<?> execute(@RequestParam String releaseId) {
        return opsApiService.executeRelease(releaseId);
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasAuthority('ops:release:rollback')")
    public Result<?> rollback(@RequestParam String releaseId) {
        return opsApiService.rollbackRelease(releaseId);
    }
}
