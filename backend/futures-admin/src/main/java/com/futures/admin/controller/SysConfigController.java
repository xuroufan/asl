package com.futures.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysConfig;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统管理 — 参数配置
 */
@RestController
@RequestMapping("/api/v1/admin/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysUserService userService;

    @GetMapping("/list")
    @PreAuthorize("hasPermission('system:config:list')")
    public Result<PageResult<SysConfig>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        IPage<SysConfig> pr = userService.getConfigPage(page, size);
        return Result.success(PageResult.from(pr));
    }

    @GetMapping("/{configId}")
    @PreAuthorize("hasPermission('system:config:list')")
    public Result<SysConfig> getById(@PathVariable Long configId) {
        return Result.success(userService.getConfigById(configId));
    }

    /** 根据 configKey 获取值（公开接口） */
    @GetMapping("/key/{configKey}")
    public Result<String> getByKey(@PathVariable String configKey) {
        return Result.success(userService.getConfigValueByKey(configKey));
    }

    @PostMapping
    @PreAuthorize("hasPermission('system:config:add')")
    @Log(title = "参数配置", operType = 1)
    public Result<Void> add(@RequestBody SysConfig config) {
        userService.saveConfig(config);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasPermission('system:config:edit')")
    @Log(title = "参数配置", operType = 2)
    public Result<Void> edit(@RequestBody SysConfig config) {
        userService.saveConfig(config);
        return Result.success();
    }

    @DeleteMapping("/{configId}")
    @PreAuthorize("hasPermission('system:config:remove')")
    @Log(title = "参数配置", operType = 3)
    public Result<Void> remove(@PathVariable Long configId) {
        userService.removeConfigById(configId);
        return Result.success();
    }
}
