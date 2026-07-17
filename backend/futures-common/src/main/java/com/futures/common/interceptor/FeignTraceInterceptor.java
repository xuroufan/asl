package com.futures.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器 — 跨服务传递 TraceId
 *
 * 从 MDC 中读取 traceId，注入到 Feign 请求头 X-Trace-Id，
 * 下游微服务通过 TraceIdFilter 接收并写入 MDC。
 */
@Slf4j
@Configuration
public class FeignTraceInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";

    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return (RequestTemplate template) -> {
            // 1. 优先从 MDC 获取
            String traceId = MDC.get(MDC_TRACE_ID);

            // 2. MDC 没有时从当前请求获取
            if (traceId == null || traceId.isBlank()) {
                RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
                if (attrs instanceof ServletRequestAttributes) {
                    HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
                    traceId = (String) req.getAttribute("X-Trace-Id");
                    if (traceId == null) {
                        traceId = req.getHeader(TRACE_ID_HEADER);
                    }
                }
            }

            // 3. 还没有则生成新的（兜底）
            if (traceId == null || traceId.isBlank()) {
                traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            template.header(TRACE_ID_HEADER, traceId);
        };
    }
}
