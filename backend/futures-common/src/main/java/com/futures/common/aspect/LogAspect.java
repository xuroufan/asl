package com.futures.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 接口请求日志切面
 *
 * 记录所有 Controller 方法的请求参数、响应耗时。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.futures..controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String method = "";
        String uri = "";
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
        }

        // 构建请求日志
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = point.getSignature().getName();
        String args = Arrays.stream(point.getArgs())
                .map(a -> a == null ? "null" : a.toString())
                .collect(Collectors.joining(", "));

        log.info("[REQ] {} {} #{}.{}({})", method, uri, className, methodName, args);

        try {
            Object result = point.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[RES] {} {} #{} cost={}ms", method, uri, className, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[ERR] {} {} #{} cost={}ms error={}", method, uri, className, elapsed, e.getMessage());
            throw e;
        }
    }
}
