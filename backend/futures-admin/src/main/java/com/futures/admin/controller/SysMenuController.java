package com.futures.admin.controller;

import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysMenu;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 — 菜单管理
 */
@RestController
@RequestMapping("/api/v1/admin/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysUserService userService;

    /** 查询菜单树（管理用，显示全部菜单） */
    @GetMapping("/list")
    @PreAuthorize("hasPermission('system:menu:list')")
    public Result<List<SysMenu>> list() {
        return Result.success(userService.getMenuTree());
    }

    /** 根据角色 ID 查询已分配的菜单 ID */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasPermission('system:menu:list')")
    public Result<List<Long>> getByRole(@PathVariable Long roleId) {
        return Result.success(userService.getMenuIdsByRoleId(roleId));
    }

    /** 根据 ID 查询菜单 */
    @GetMapping("/{menuId}")
    @PreAuthorize("hasPermission('system:menu:list')")
    public Result<SysMenu> getById(@PathVariable Long menuId) {
        return Result.success(userService.getMenuById(menuId));
    }

    /** 新增菜单 */
    @PostMapping
    @PreAuthorize("hasPermission('system:menu:add')")
    @Log(title = "菜单管理", operType = 1)
    public Result<Void> add(@RequestBody SysMenu menu) {
        userService.saveMenu(menu);
        return Result.success();
    }

    /** 修改菜单 */
    @PutMapping
    @PreAuthorize("hasPermission('system:menu:edit')")
    @Log(title = "菜单管理", operType = 2)
    public Result<Void> edit(@RequestBody SysMenu menu) {
        userService.saveMenu(menu);
        return Result.success();
    }

    /** 删除菜单 */
    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasPermission('system:menu:remove')")
    @Log(title = "菜单管理", operType = 3)
    public Result<Void> remove(@PathVariable Long menuId) {
        userService.removeMenuById(menuId);
        return Result.success();
    }
}
