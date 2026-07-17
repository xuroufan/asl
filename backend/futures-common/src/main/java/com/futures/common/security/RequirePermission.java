package com.futures.common.security;

import java.lang.annotation.*;

/**
 * 接口级权限校验注解。
 * <p>
 * 标注在 Controller 方法上，配合 {@link PermissionAspect} 切面实现 RBAC 权限校验。
 * </p>
 *
 * 使用示例：
 * <pre>
 * @RequirePermission("order:create")
 * public Result&lt;String&gt; placeOrder(@RequestBody OrderPlaceRequest request, @RequestHeader("X-User-Id") Long userId) {
 *     // ...
 * }
 *
 * @RequirePermission(value = "order:cancel", requireAdmin = true)
 * public Result&lt;Void&gt; adminCancelOrder(@PathVariable String orderId) {
 *     // ...
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 所需权限编码。
     * <p>格式：{领域}:{操作}，如 {@code "order:create"}、{@code "fund:withdraw"}</p>
     */
    String value();

    /**
     * 是否要求管理员角色。
     * <p>若为 true，除检查 value 权限外，还会校验角色是否包含 ADMIN。</p>
     */
    boolean requireAdmin() default false;

    /**
     * 校验失败时的错误提示。
     */
    String message() default "权限不足，无法执行此操作";
}
