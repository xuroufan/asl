package com.futures.admin.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 标注在 Controller 方法上自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /** 操作模块标题 */
    String title() default "";
    /** 操作类型：0=其他 1=新增 2=修改 3=删除 4=查询 5=登录 */
    int operType() default 0;
}
