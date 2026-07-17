package com.futures.account.controller;

import com.futures.account.entity.UserEntity;
import com.futures.account.service.UserService;
import com.futures.common.result.Result;
import com.futures.common.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 权限管理控制器（仅 ADMIN 角色可操作）。
 * <p>
 * 提供用户交易权限的查询、分配和修改功能。
 * 权限以逗号分隔字符串存储在 t_user.trading_permissions 字段。
 * </p>
 *
 * 权限编码规则：{领域}:{操作}
 * <ul>
 *   <li>order:create — 下单</li>
 *   <li>order:cancel — 撤单</li>
 *   <li>order:query — 查询订单</li>
 *   <li>fund:view — 查看资金</li>
 *   <li>fund:withdraw — 出金</li>
 *   <li>fund:deposit — 入金</li>
 *   <li>position:view — 查看持仓</li>
 *   <li>position:close — 平仓</li>
 *   <li>admin:* — 管理员权限（通配）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission")
public class PermissionController {

    private final UserService userService;

    /**
     * 查询指定用户的交易权限列表。
     */
    @GetMapping("/{userId}")
    @RequirePermission(value = "admin:permission:view", requireAdmin = true)
    public Result<String> getUserPermissions(@PathVariable Long userId) {
        return Result.success(userService.getUserPermissions(userId));
    }

    /**
     * 设置用户的交易权限。
     * <p>body 示例: { "permissions": "order:create,order:cancel,fund:view" }</p>
     */
    @PutMapping("/{userId}")
    @RequirePermission(value = "admin:permission:update", requireAdmin = true)
    public Result<String> setUserPermissions(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String permissions = body.get("permissions");
        userService.setUserPermissions(userId, permissions);
        log.info("管理员更新用户 {} 权限: {}", userId, permissions);
        return Result.success("权限已更新");
    }

    /**
     * 修改用户角色。
     * <p>body 示例: { "role": "USER" } — 支持 USER, VIP, ADMIN</p>
     */
    @PutMapping("/{userId}/role")
    @RequirePermission(value = "admin:role:update", requireAdmin = true)
    public Result<String> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        userService.updateUserRole(userId, role);
        log.info("管理员更新用户 {} 角色: {}", userId, role);
        return Result.success("角色已更新");
    }
}
