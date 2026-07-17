package com.futures.common.interceptor;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器 — 全链路追踪标识
 *
 * 为每个请求生成唯一 traceId，写入 MDC，下游微服务和异步线程继承。
 * 前端可携带 X-Trace-Id 头来串联前后端日志。
 *
 * 传递链路：
 *   前端 → Gateway (X-Trace-Id) → Feign (X-Trace-Id) → 微服务 → Async → Log
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            // 1. 尝试从前端请求头获取 traceId
            String traceId = req.getHeader(TRACE_ID_HEADER);

            // 2. 如果没有，生成新的
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // 3. 写入 MDC（日志框架自动拾取）
            MDC.put(MDC_TRACE_ID, traceId);

            // 4. 响应头返回 traceId（供前端后续请求携带）
            resp.setHeader(TRACE_ID_HEADER, traceId);

            // 5. 传递给下游（通过 Request Attribute 供 Feign 拦截器读取）
            req.setAttribute(TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            // 请求结束后清除 MDC，防止线程复用导致 traceId 错乱
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
