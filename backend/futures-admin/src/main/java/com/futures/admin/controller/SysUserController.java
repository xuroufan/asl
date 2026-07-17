package com.futures.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysRole;
import com.futures.admin.entity.SysUser;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统管理 — 用户管理
 */
@RestController
@RequestMapping("/api/v1/admin/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询用户列表 */
    @GetMapping("/list")
    @PreAuthorize("hasPermission('system:user:list')")
    @Log(title = "用户管理", operType = 4)
    public Result<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            SysUser query) {
        IPage<SysUser> pageResult = userService.getUserPage(page, size, query);
        // 隐藏密码
        pageResult.getRecords().forEach(u -> u.setPassword(""));
        return Result.success(PageResult.from(pageResult));
    }

    /** 根据 ID 查询用户 */
    @GetMapping("/{userId}")
    @PreAuthorize("hasPermission('system:user:list')")
    public Result<SysUser> getById(@PathVariable Long userId) {
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword("");
            // 加载角色 IDs
            List<SysRole> roles = userService.getUserRoles(userId);
            user.setRoleIds(roles.stream().map(SysRole::getRoleId).toArray(Long[]::new));
        }
        return Result.success(user);
    }

    /** 新增用户 */
    @PostMapping
    @PreAuthorize("hasPermission('system:user:add')")
    @Log(title = "用户管理", operType = 1)
    public Result<Void> add(@RequestBody SysUser user) {
        // 检查用户名唯一
        SysUser exist = userService.getUserByUsername(user.getUsername());
        if (exist != null) {
            return Result.error(400, "用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);
        return Result.success();
    }

    /** 修改用户 */
    @PutMapping
    @PreAuthorize("hasPermission('system:user:edit')")
    @Log(title = "用户管理", operType = 2)
    public Result<Void> edit(@RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.saveUser(user);
        return Result.success();
    }

    /** 删除用户 */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasPermission('system:user:remove')")
    @Log(title = "用户管理", operType = 3)
    public Result<Void> remove(@PathVariable Long userId) {
        userService.removeById(userId);
        return Result.success();
    }

    /** 重置密码 */
    @PutMapping("/resetPwd/{userId}")
    @PreAuthorize("hasPermission('system:user:edit')")
    @Log(title = "用户管理", operType = 2)
    public Result<Void> resetPwd(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return Result.success();
    }

    /** 修改用户状态 */
    @PutMapping("/status")
    @PreAuthorize("hasPermission('system:user:edit')")
    @Log(title = "用户管理", operType = 2)
    public Result<Void> changeStatus(@RequestBody SysUser user) {
        userService.updateById(user);
        return Result.success();
    }
}
