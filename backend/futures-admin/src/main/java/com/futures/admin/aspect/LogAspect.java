package com.futures.admin.aspect;

import com.futures.admin.entity.SysOperLog;
import com.futures.admin.security.LoginUser;
import com.futures.admin.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 操作日志切面 — 自动记录 Controller 层操作日志
 */
@Slf4j
@Aspect
@Component("adminLogAspect")
@RequiredArgsConstructor
public class LogAspect {

    private static final String BEAN_NAME = "adminLogAspect";

    private final SysUserService userService;

    /** 定义切入点：所有带有 @Log 注解的方法 */
    @Pointcut("@annotation(com.futures.admin.aspect.Log)")
    public void logPointcut() {}

    /** 正常返回后记录日志 */
    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, result, null);
    }

    /** 异常后记录日志 */
    @AfterThrowing(pointcut = "logPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, null, e);
    }

    private void handleLog(JoinPoint joinPoint, Object result, Exception e) {
        try {
            // 获取 @Log 注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnno = method.getAnnotation(Log.class);
            if (logAnno == null) return;

            // 构建操作日志对象
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(logAnno.title());
            operLog.setOperType(logAnno.operType());
            operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
            operLog.setRequestMethod(getRequest().getMethod());
            operLog.setOperUrl(getRequest().getRequestURI());
            operLog.setOperIp(getRequest().getRemoteAddr());

            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) auth.getPrincipal();
                operLog.setOperName(loginUser.getUsername());
            }

            // 请求参数
            operLog.setOperParam(Arrays.toString(joinPoint.getArgs()));

            // 执行耗时
            long costTime = 0; // cost time would be calculated from a start time stored in request
            operLog.setCostTime(costTime);

            // 异常信息
            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(e.getMessage() != null ? (e.getMessage().length() > 2000 ? e.getMessage().substring(0, 2000) : e.getMessage()) : "");
            } else {
                operLog.setStatus(0);
                if (result != null) {
                    operLog.setJsonResult(result.toString());
                }
            }

            // 异步保存日志
            userService.saveOperLog(operLog);
        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        }
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
