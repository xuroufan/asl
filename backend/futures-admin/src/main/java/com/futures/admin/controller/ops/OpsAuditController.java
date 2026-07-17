package com.futures.admin.controller.ops;

import com.futures.admin.client.OpsApiService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统操作审计控制器。
 * <p>记录所有运维操作（发布、配置变更、扩缩容等），支持追溯。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/ops/audit")
@RequiredArgsConstructor
public class OpsAuditController {

    private final OpsApiService opsApiService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ops:audit:list')")
    public Result<?> list(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return opsApiService.getAuditLogs(operator, action, module, startTime, endTime, page, size);
    }
}
