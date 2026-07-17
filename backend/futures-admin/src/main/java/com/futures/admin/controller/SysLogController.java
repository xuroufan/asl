package com.futures.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.admin.entity.SysLoginLog;
import com.futures.admin.entity.SysOperLog;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统监控 — 日志查询
 */
@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
public class SysLogController {

    private final SysUserService userService;

    /** 操作日志分页 */
    @GetMapping("/operlog/list")
    @PreAuthorize("hasPermission('monitor:operlog:list')")
    public Result<PageResult<SysOperLog>> operLogList(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        IPage<SysOperLog> pr = userService.getOperLogPage(page, size);
        return Result.success(PageResult.from(pr));
    }

    /** 删除操作日志 */
    @DeleteMapping("/operlog/{operId}")
    @PreAuthorize("hasPermission('monitor:operlog:remove')")
    public Result<Void> operLogRemove(@PathVariable Long operId) {
        userService.removeOperLogById(operId);
        return Result.success();
    }

    /** 清空操作日志 */
    @DeleteMapping("/operlog/clean")
    @PreAuthorize("hasPermission('monitor:operlog:remove')")
    public Result<Void> operLogClean() {
        userService.cleanOperLog();
        return Result.success();
    }

    /** 登录日志分页 */
    @GetMapping("/loginlog/list")
    @PreAuthorize("hasPermission('monitor:loginlog:list')")
    public Result<PageResult<SysLoginLog>> loginLogList(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) String username) {
        IPage<SysLoginLog> pr = userService.getLoginLogPage(page, size, username);
        return Result.success(PageResult.from(pr));
    }

    /** 删除登录日志 */
    @DeleteMapping("/loginlog/{infoId}")
    @PreAuthorize("hasPermission('monitor:loginlog:remove')")
    public Result<Void> loginLogRemove(@PathVariable Long infoId) {
        userService.removeLoginLogById(infoId);
        return Result.success();
    }

    /** 清空登录日志 */
    @DeleteMapping("/loginlog/clean")
    @PreAuthorize("hasPermission('monitor:loginlog:remove')")
    public Result<Void> loginLogClean() {
        userService.cleanLoginLog();
        return Result.success();
    }
}
