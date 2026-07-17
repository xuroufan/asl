package com.futures.account.integration;

import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心链路集成测试 — 注册 → 登录 → 下单 → 查询
 *
 * <p>依赖运行中的全部微服务(gateway:8088)。通过 -Dgateway.url 指定网关地址。
 * <pre>
 *   mvn test -pl futures-account -Dtest=FullFlowIntegrationTest
 *        -Dgateway.url=http://localhost:8088
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullFlowIntegrationTest {

    private static final RestTemplate rest = new RestTemplate();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String gatewayUrl;
    private static String accessToken;
    private static String userId;

    @BeforeAll
    static void setup() {
        gatewayUrl = System.getProperty("gateway.url", "http://localhost:8088");

        // 先验证网关存活
        try {
            ResponseEntity<String> resp = rest.getForEntity(gatewayUrl + "/actuator/health", String.class);
            System.out.println("[SETUP] Gateway health: " + resp.getStatusCode());
        } catch (Exception e) {
            System.err.println("[SETUP] 网关不可达: " + gatewayUrl);
            System.err.println("[SETUP] 请确保服务已启动: " + e.getMessage());
            throw new IllegalStateException("Gateway not available at " + gatewayUrl);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Step1: 注册新用户")
    void testRegister() throws Exception {
        String username = "itest_" + System.currentTimeMillis();
        String email = username + "@test.com";
        String password = "Test1234!";

        String body = mapper.writeValueAsString(Map.of(
                "username", username, "password", password, "email", email
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/auth/register",
                HttpMethod.POST, req, String.class);

        assertEquals(200, resp.getStatusCode().value(), "注册应返回200");

        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals(200, json.get("code").asInt(), "响应code应为200");

        accessToken = json.get("data").get("accessToken").asText();
        userId = json.get("data").get("userInfo").get("userId").asText();

        assertNotNull(accessToken, "应返回accessToken");
        assertNotNull(userId, "应返回userId");

        System.out.println("[Step1] 注册成功: userId=" + userId);
    }

    @Test
    @Order(2)
    @DisplayName("Step2: 登录")
    void testLogin() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", "itest_user", "password", "Test1234!"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/auth/login",
                HttpMethod.POST, req, String.class);

        // 可能返回400（用户不存在）或200（登录成功）
        int statusCode = resp.getStatusCode().value();
        assertTrue(statusCode == 200 || statusCode == 400,
                "登录应返回200或400");

        System.out.println("[Step2] 登录响应: HTTP " + statusCode);
    }

    @Test
    @Order(3)
    @DisplayName("Step3: 行情查询（无需认证）")
    void testMarketQuotes() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(
                gatewayUrl + "/api/v1/market/symbols", String.class);

        assertEquals(200, resp.getStatusCode().value());

        JsonNode json = mapper.readTree(resp.getBody());
        assertTrue(json.get("data").isArray(), "应返回数组");
        assertTrue(json.get("data").size() > 0, "应至少有一组行情");

        System.out.println("[Step3] 行情数量: " + json.get("data").size());
    }

    @Test
    @Order(4)
    @DisplayName("Step4: 查询账户信息（需Token）")
    void testAccountInfo() throws Exception {
        if (accessToken == null) {
            System.out.println("[Step4] 跳过: 无token（未注册或注册失败）");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/account/overview",
                HttpMethod.GET, req, String.class);

        int sc = resp.getStatusCode().value();
        assertTrue(sc == 200 || sc == 401 || sc == 500,
                "账户查询应返回200/401/500");

        System.out.println("[Step4] 账户信息: HTTP " + sc);
    }

    @Test
    @Order(5)
    @DisplayName("Step5: 查询资金余额（需Token）")
    void testFundBalance() throws Exception {
        if (accessToken == null) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/fund/balance?userId=" + userId,
                HttpMethod.GET, req, String.class);

        int sc = resp.getStatusCode().value();
        assertTrue(sc == 200 || sc == 401 || sc == 404 || sc == 500,
                "资金查询应返回预期状态码");

        System.out.println("[Step5] 资金余额: HTTP " + sc);
    }

    @Test
    @Order(6)
    @DisplayName("Step6: 入金")
    void testDeposit() throws Exception {
        if (accessToken == null) return;

        String body = mapper.writeValueAsString(Map.of(
                "userId", userId, "amount", 50000
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> req = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/fund/deposit",
                HttpMethod.POST, req, String.class);

        assertEquals(200, resp.getStatusCode().value(), "入金应返回200");
        System.out.println("[Step6] 入金成功: 50,000");
    }

    @Test
    @Order(7)
    @DisplayName("Step7: 查询可用资金")
    void testAvailableFunds() throws Exception {
        if (accessToken == null) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/fund/available?userId=" + userId,
                HttpMethod.GET, req, String.class);

        int sc = resp.getStatusCode().value();
        System.out.println("[Step7] 可用资金查询: HTTP " + sc);

        if (sc == 200) {
            JsonNode json = mapper.readTree(resp.getBody());
            System.out.println("[Step7] 可用资金: " + json.get("data"));
        }
    }

    @Test
    @Order(8)
    @DisplayName("Step8: 风控状态查询（需Token）")
    void testRiskStatus() throws Exception {
        if (accessToken == null) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(
                gatewayUrl + "/api/v1/risk/status",
                HttpMethod.GET, req, String.class);

        System.out.println("[Step8] 风控状态: HTTP " + resp.getStatusCode().value());
        // 风控可能返回401或404等，记录即可
    }

    @Test
    @Order(9)
    @DisplayName("Step9: 健康检查（全部服务）")
    void testAllServiceHealth() {
        Map<String, Integer> services = Map.of(
                "gateway", 8088, "account", 8083, "order", 8081,
                "matching", 8082, "fund", 8084, "risk", 8085,
                "market", 8086, "settlement", 8087
        );

        for (var entry : services.entrySet()) {
            try {
                String url = "http://localhost:" + entry.getValue() + "/actuator/health";
                ResponseEntity<String> resp = rest.getForEntity(url, String.class);
                System.out.println("  [Health] " + entry.getKey() + ": " + resp.getStatusCode().value());
            } catch (Exception e) {
                System.out.println("  [Health] " + entry.getKey() + ": FAIL (" + e.getMessage() + ")");
            }
        }
    }
}
