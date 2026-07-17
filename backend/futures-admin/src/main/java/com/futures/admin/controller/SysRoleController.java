package com.futures.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysRole;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 — 角色管理
 */
@RestController
@RequestMapping("/api/v1/admin/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysUserService userService;

    /** 分页查询角色列表 */
    @GetMapping("/list")
    @PreAuthorize("hasPermission('system:role:list')")
    public Result<PageResult<SysRole>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        IPage<SysRole> pageResult = userService.getRolePage(page, size);
        return Result.success(PageResult.from(pageResult));
    }

    /** 查询所有角色（下拉选择用） */
    @GetMapping("/all")
    @PreAuthorize("hasPermission('system:role:list')")
    public Result<List<SysRole>> all() {
        return Result.success(userService.getAllRoles());
    }

    /** 根据 ID 查询角色 */
    @GetMapping("/{roleId}")
    @PreAuthorize("hasPermission('system:role:list')")
    public Result<SysRole> getById(@PathVariable Long roleId) {
        SysRole role = userService.getRoleById(roleId);
        if (role != null) {
            role.setMenuIds(userService.getMenuIdsByRoleId(roleId).toArray(new Long[0]));
        }
        return Result.success(role);
    }

    /** 新增角色 */
    @PostMapping
    @PreAuthorize("hasPermission('system:role:add')")
    @Log(title = "角色管理", operType = 1)
    public Result<Void> add(@RequestBody SysRole role) {
        userService.saveRole(role);
        return Result.success();
    }

    /** 修改角色 */
    @PutMapping
    @PreAuthorize("hasPermission('system:role:edit')")
    @Log(title = "角色管理", operType = 2)
    public Result<Void> edit(@RequestBody SysRole role) {
        userService.saveRole(role);
        return Result.success();
    }

    /** 删除角色 */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasPermission('system:role:remove')")
    @Log(title = "角色管理", operType = 3)
    public Result<Void> remove(@PathVariable Long roleId) {
        userService.removeRoleById(roleId);
        return Result.success();
    }
}
