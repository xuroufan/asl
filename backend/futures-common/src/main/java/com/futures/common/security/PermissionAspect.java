package com.futures.common.security;

import com.futures.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 权限校验切面。
 * <p>
 * 拦截所有标注 {@link RequirePermission} 的 Controller 方法，
 * 从请求头 {@code X-User-Id} 和 {@code X-User-Roles} 读取用户信息，
 * 校验用户是否拥有指定权限。
 * </p>
 *
 * 权限校验流程：
 * <ol>
 *   <li>从请求头读取用户角色和权限信息（由网关注入）</li>
 *   <li>若 requireAdmin=true，检查角色是否包含 ADMIN</li>
 *   <li>检查用户的权限列表是否包含注解的 value</li>
 *   <li>校验失败抛出 BizException，由全局异常处理器返回 403</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        String roles = request.getHeader("X-User-Roles");
        String permissions = request.getHeader("X-User-Permissions");

        // 没有用户信息（可能未经过网关或旧版本客户端）
        if (userId == null || userId.isEmpty()) {
            throw BizException.unauthorized("未登录或 Token 已过期");
        }

        // 验证 ADMIN 角色（如果注解要求管理员）
        if (requirePermission.requireAdmin()) {
            if (roles == null || !List.of(roles.split(",")).contains("ADMIN")) {
                log.warn("用户 {} 权限不足：需要 ADMIN 角色", userId);
                throw BizException.forbidden(requirePermission.message());
            }
        }

        // 验证具体权限
        String required = requirePermission.value();
        if (required != null && !required.isEmpty()) {
            if (permissions == null || permissions.isEmpty()) {
                log.warn("用户 {} 权限不足：缺少权限 {}", userId, required);
                throw BizException.forbidden(requirePermission.message());
            }
            // 支持星号通配符（如 "order:*" 表示所有 order 权限）
            List<String> userPerms = List.of(permissions.split(","));
            boolean hasPermission = userPerms.contains(required)
                    || userPerms.stream().anyMatch(p -> {
                        if (p.endsWith(":*")) {
                            String domain = p.replace(":*", "");
                            return required.startsWith(domain);
                        }
                        return false;
                    });
            if (!hasPermission) {
                log.warn("用户 {} 权限不足：需要 {}，用户拥有 [{}]", userId, required, permissions);
                throw BizException.forbidden(requirePermission.message());
            }
        }

        return joinPoint.proceed();
    }
}
