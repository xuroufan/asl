package com.futures.admin.controller;

import com.futures.admin.service.SysUserService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SysUserService userService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(userService.getDashboardStats());
    }
}
