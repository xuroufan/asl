package com.futures.admin.controller;

import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysDept;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 — 部门管理
 */
@RestController
@RequestMapping("/api/v1/admin/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysUserService userService;

    /** 查询部门树 */
    @GetMapping("/list")
    @PreAuthorize("hasPermission('system:dept:list')")
    public Result<List<SysDept>> list() {
        return Result.success(userService.getDeptTree());
    }

    /** 根据 ID 查询部门 */
    @GetMapping("/{deptId}")
    @PreAuthorize("hasPermission('system:dept:list')")
    public Result<SysDept> getById(@PathVariable Long deptId) {
        return Result.success(userService.getDeptById(deptId));
    }

    /** 新增部门 */
    @PostMapping
    @PreAuthorize("hasPermission('system:dept:add')")
    @Log(title = "部门管理", operType = 1)
    public Result<Void> add(@RequestBody SysDept dept) {
        userService.saveDept(dept);
        return Result.success();
    }

    /** 修改部门 */
    @PutMapping
    @PreAuthorize("hasPermission('system:dept:edit')")
    @Log(title = "部门管理", operType = 2)
    public Result<Void> edit(@RequestBody SysDept dept) {
        userService.saveDept(dept);
        return Result.success();
    }

    /** 删除部门 */
    @DeleteMapping("/{deptId}")
    @PreAuthorize("hasPermission('system:dept:remove')")
    @Log(title = "部门管理", operType = 3)
    public Result<Void> remove(@PathVariable Long deptId) {
        userService.removeDeptById(deptId);
        return Result.success();
    }
}
