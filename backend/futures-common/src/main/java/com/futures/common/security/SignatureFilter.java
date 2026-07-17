package com.futures.common.security;

import com.futures.common.exception.BizException;
import com.futures.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API 请求签名校验过滤器。
 * <p>
 * 对接口请求进行 HMAC-SHA256 签名校验，防止重放攻击。
 * 客户端需在请求头中携带：X-Timestamp（毫秒时间戳）、X-Nonce（随机字符串）、X-Sign（签名值）、X-App-Id（应用ID）。
 * </p>
 *
 * 该过滤器默认关闭，需在 application.yml 中配置 futures.security.signature.enabled=true 启用。
 */
@Slf4j
@Component
@Order(-50)
public class SignatureFilter implements Filter {

    /** 放行路径（公共接口无需签名验证） */
    private static final java.util.List<String> WHITE_LIST = java.util.List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/captcha",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/prometheus",
            "/swagger-ui",
            "/v3/api-docs"
    );

    /**
     * 配置签名校验是否启用。
     * <p>默认关闭，由 {@code futures.security.signature.enabled} 控制。</p>
     */
    private boolean enabled = false;
    private String appSecret = "";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        // 签名校验未启用则跳过
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        // 提取签名请求头
        String timestampStr = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");
        String appId = request.getHeader("X-App-Id");

        if (timestampStr == null || nonce == null || sign == null) {
            writeError(response, "缺少签名请求头（X-Timestamp, X-Nonce, X-Sign）");
            return;
        }

        // 解析时间戳
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            writeError(response, "X-Timestamp 格式无效");
            return;
        }

        // 读取请求体
        String body = "";
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            body = sb.toString();
        }

        // 验证签名
        boolean valid = SignatureUtil.verify(appSecret, method, path, timestamp, nonce, body, sign);
        if (!valid) {
            writeError(response, "签名验证失败，请求被拒绝");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(Result.unauthorized(msg));
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
