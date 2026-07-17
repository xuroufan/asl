package com.futures.admin.client;

import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 风控服务 API 客户端 — 封装对 futures-risk 微服务的 REST 调用。
 * <p>所有 API 从 futures-risk 的 RiskController 代理而来。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskApiService {

    private final RestTemplate restTemplate;

    /** futures-risk 服务的基础 URL，从配置文件读取 */
    @Value("${risk.service.url:http://localhost:8085}")
    private String riskServiceUrl;

    // ==================== 风控配置管理 ====================

    /**
     * 查询所有风控配置。
     */
    public Result<List<Map<String, Object>>> listRiskConfigs() {
        String url = riskServiceUrl + "/api/v1/risk/config/list";
        return restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {}
        ).getBody();
    }

    /**
     * 更新风控配置（管理员）。
     */
    public Result<String> updateRiskConfig(Map<String, Object> config) {
        String url = riskServiceUrl + "/api/v1/risk/config";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(config);
        return restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Result<String>>() {}
        ).getBody();
    }

    /**
     * 刷新风控配置缓存。
     */
    public Result<String> refreshConfig() {
        String url = riskServiceUrl + "/api/v1/risk/config/refresh";
        return restTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<Result<String>>() {}
        ).getBody();
    }

    // ==================== 风控预警 ====================

    /**
     * 查询风控预警记录（分页）。
     */
    public Result<PageResult<Map<String, Object>>> getRiskAlerts(
            Long userId, String alertType, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(riskServiceUrl + "/api/v1/risk/alert/list")
                .queryParam("page", page)
                .queryParam("size", size);
        if (userId != null) builder.queryParam("userId", userId);
        if (alertType != null && !alertType.isEmpty()) builder.queryParam("alertType", alertType);

        String url = builder.build().toUriString();
        return restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<PageResult<Map<String, Object>>>>() {}
        ).getBody();
    }

    // ==================== 强平记录 ====================

    /**
     * 查询强平记录（分页）。
     */
    public Result<PageResult<Map<String, Object>>> getLiquidationRecords(
            Long userId, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(riskServiceUrl + "/api/v1/risk/liquidation/records")
                .queryParam("page", page)
                .queryParam("size", size);
        if (userId != null) builder.queryParam("userId", userId);

        String url = builder.build().toUriString();
        return restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<PageResult<Map<String, Object>>>>() {}
        ).getBody();
    }

    // ==================== 风控状态 ====================

    /**
     * 获取用户风控状态总览。
     */
    public Result<Map<String, Object>> getUserRiskStatus(Long userId) {
        String url = riskServiceUrl + "/api/v1/risk/status?userId=" + userId;
        return restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
        ).getBody();
    }

    /**
     * 获取用户当前风险度。
     */
    public Result<BigDecimal> getCurrentRiskRatio(Long userId) {
        String url = riskServiceUrl + "/api/v1/risk/current-risk-ratio?userId=" + userId;
        return restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<BigDecimal>>() {}
        ).getBody();
    }

    // ==================== 综合数据 ====================

    /**
     * 获取风控监控大盘数据（聚合多个接口）。
     * <p>当 futures-risk 不可用时返回模拟数据。</p>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRiskDashboardData() {
        try {
            // 聚合多个接口的数据
            Result<List<Map<String, Object>>> configsResult = listRiskConfigs();
            Result<PageResult<Map<String, Object>>> alertsResult = getRiskAlerts(null, null, 1, 20);
            Result<PageResult<Map<String, Object>>> liquidationsResult = getLiquidationRecords(null, 1, 20);

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("totalConfigs", configsResult != null && configsResult.getData() != null
                    ? configsResult.getData().size() : 0);
            data.put("recentAlerts", alertsResult != null && alertsResult.getData() != null
                    ? alertsResult.getData().getRecords() : List.of());
            data.put("recentLiquidations", liquidationsResult != null && liquidationsResult.getData() != null
                    ? liquidationsResult.getData().getRecords() : List.of());

            return data;
        } catch (Exception e) {
            log.warn("获取风控大盘数据失败，返回模拟数据: {}", e.getMessage());
            return getMockDashboardData();
        }
    }

    /**
     * 当 futures-risk 不可用时返回模拟数据。
     */
    private Map<String, Object> getMockDashboardData() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("totalConfigs", 8);
        data.put("totalAlerts", 23);
        data.put("totalLiquidations", 5);
        data.put("averageRiskRatio", 62.5);
        data.put("highRiskUsers", 12);
        data.put("todayLiquidations", 2);

        // 模拟风险度分布
        List<Map<String, Object>> riskDistribution = List.of(
                Map.of("name", "< 50%", "value", 120),
                Map.of("name", "50-80%", "value", 65),
                Map.of("name", "80-100%", "value", 28),
                Map.of("name", "> 100%", "value", 12)
        );
        data.put("riskDistribution", riskDistribution);

        // 模拟高风险用户
        List<Map<String, Object>> highRiskUsers = List.of(
                Map.of("userId", 1001, "username", "test_user_01", "riskRatio", 95.3, "symbol", "HSI"),
                Map.of("userId", 1002, "username", "test_user_02", "riskRatio", 88.7, "symbol", "ES"),
                Map.of("userId", 1003, "username", "test_user_03", "riskRatio", 82.1, "symbol", "CL")
        );
        data.put("highRiskUsers", highRiskUsers);

        return data;
    }
}
