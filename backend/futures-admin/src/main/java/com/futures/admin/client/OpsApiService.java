package com.futures.admin.client;

import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 运维管理平台 API 客户端 — 封装对基础设施服务的 REST 调用（降级模式）。
 * <p>所有 API 以模拟数据为主，用于运维管理后台的前端展示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsApiService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 服务状态监控 ====================

    public Result<List<Map<String, Object>>> getServices() {
        return Result.success(buildMockServices());
    }

    public Result<List<Map<String, Object>>> getServiceInstances(String serviceName) {
        return Result.success(buildMockInstances(serviceName));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServiceDashboard() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalServices", 8);
            data.put("healthyServices", 7);
            data.put("unhealthyServices", 1);
            data.put("totalInstances", 16);
            data.put("overallSuccessRate", 99.2);
            data.put("avgResponseTime", 45);
            data.put("services", buildMockServices());

            // 依赖拓扑（简化版）
            data.put("dependencies", List.of(
                    Map.of("from", "futures-gateway", "to", "futures-order"),
                    Map.of("from", "futures-gateway", "to", "futures-account"),
                    Map.of("from", "futures-gateway", "to", "futures-market"),
                    Map.of("from", "futures-order", "to", "futures-matching"),
                    Map.of("from", "futures-order", "to", "futures-fund"),
                    Map.of("from", "futures-order", "to", "futures-risk"),
                    Map.of("from", "futures-fund", "to", "futures-settlement"),
                    Map.of("from", "futures-matching", "to", "futures-market")
            ));

            return data;
        } catch (Exception e) {
            log.warn("获取服务大盘数据失败: {}", e.getMessage());
            return getMockServiceDashboard();
        }
    }

    // ==================== 服务发布管理 ====================

    public Result<List<Map<String, Object>>> getReleaseHistory(int page, int size) {
        return Result.success(buildMockReleaseHistory(page));
    }

    public Result<Map<String, Object>> getReleaseDetail(String releaseId) {
        return Result.success(buildMockReleaseDetail(releaseId));
    }

    public Result<String> createRelease(Map<String, Object> release) {
        log.info("创建发布单: {}", release);
        return Result.success("发布单已创建，编号: RL-" + System.currentTimeMillis());
    }

    public Result<String> approveRelease(String releaseId, String comment) {
        log.info("审批发布单: {} -> {}", releaseId, comment);
        return Result.success("发布单 " + releaseId + " 已" + ("approve".equals(comment) ? "批准" : "驳回"));
    }

    public Result<String> executeRelease(String releaseId) {
        log.info("执行发布: {}", releaseId);
        return Result.success("发布 " + releaseId + " 已开始执行");
    }

    public Result<String> rollbackRelease(String releaseId) {
        log.info("回滚发布: {}", releaseId);
        return Result.success("发布 " + releaseId + " 已回滚至上一版本");
    }

    public Result<Map<String, Object>> getReleaseStats() {
        return Result.success(getMockReleaseStats());
    }

    // ==================== 配置变更管理 ====================

    public Result<List<Map<String, Object>>> getNacosConfigs(String serviceName, String env) {
        return Result.success(buildMockConfigs(serviceName, env));
    }

    public Result<Map<String, Object>> getConfigDetail(String configId) {
        return Result.success(buildMockConfigDetail(configId));
    }

    public Result<List<Map<String, Object>>> getConfigChangeHistory(String configId) {
        return Result.success(buildMockConfigChanges(configId));
    }

    public Result<String> applyConfigChange(Map<String, Object> change) {
        log.info("配置变更申请: {}", change);
        return Result.success("配置变更申请已提交，待审批");
    }

    public Result<String> approveConfigChange(String changeId, boolean approved, String comment) {
        return Result.success("配置变更 " + changeId + " 已" + (approved ? "批准" : "驳回") +
                (comment != null && !comment.isEmpty() ? "，备注: " + comment : ""));
    }

    public Result<List<Map<String, Object>>> compareConfigs(String serviceName, String envA, String envB) {
        return Result.success(buildMockConfigDiff(serviceName, envA, envB));
    }

    // ==================== 日志查询平台 ====================

    public Result<List<Map<String, Object>>> searchLogs(String serviceName, String level,
                                                        String keyword, String traceId,
                                                        String startTime, String endTime,
                                                        int page, int size) {
        return Result.success(buildMockLogs(serviceName, level, keyword, traceId, page));
    }

    public Result<List<Map<String, Object>>> getLogContext(String traceId, String timestamp) {
        return Result.success(buildMockLogContext(traceId));
    }

    public Result<Map<String, Object>> getLogStats(String serviceName, String startTime, String endTime) {
        return Result.success(getMockLogStats(serviceName));
    }

    // ==================== 告警管理 ====================

    public Result<List<Map<String, Object>>> getAlerts(String level, String status,
                                                       String serviceName, int page, int size) {
        return Result.success(buildMockAlerts(level, status, serviceName, page));
    }

    public Result<String> claimAlert(String alertId) {
        return Result.success("告警 " + alertId + " 已认领");
    }

    public Result<String> resolveAlert(String alertId, String resolution, String notes) {
        return Result.success("告警 " + alertId + " 已解决，处理方式: " + resolution +
                (notes != null && !notes.isEmpty() ? "，备注: " + notes : ""));
    }

    public Result<Map<String, Object>> getAlertStats() {
        return Result.success(getMockAlertStats());
    }

    // ==================== 系统操作审计 ====================

    public Result<List<Map<String, Object>>> getAuditLogs(String operator, String action,
                                                          String module, String startTime, String endTime,
                                                          int page, int size) {
        return Result.success(buildMockAuditLogs(operator, action, module, page));
    }

    // ==================== 模拟数据构建 ====================

    private List<Map<String, Object>> buildMockServices() {
        List<Map<String, Object>> services = List.of(
                Map.of("serviceName", "futures-gateway", "displayName", "API网关", "instances", 2,
                        "healthy", 2, "status", "RUNNING", "qps", 1250, "avgResponseTime", 12, "successRate", 99.8),
                Map.of("serviceName", "futures-order", "displayName", "订单服务", "instances", 3,
                        "healthy", 3, "status", "RUNNING", "qps", 380, "avgResponseTime", 45, "successRate", 99.5),
                Map.of("serviceName", "futures-matching", "displayName", "撮合引擎", "instances", 2,
                        "healthy", 2, "status", "RUNNING", "qps", 860, "avgResponseTime", 0.8, "successRate", 99.9),
                Map.of("serviceName", "futures-account", "displayName", "账户服务", "instances", 2,
                        "healthy", 1, "status", "DEGRADED", "qps", 120, "avgResponseTime", 68, "successRate", 98.2),
                Map.of("serviceName", "futures-fund", "displayName", "资金服务", "instances", 2,
                        "healthy", 2, "status", "RUNNING", "qps", 200, "avgResponseTime", 35, "successRate", 99.6),
                Map.of("serviceName", "futures-risk", "displayName", "风控引擎", "instances", 2,
                        "healthy", 2, "status", "RUNNING", "qps", 150, "avgResponseTime", 8, "successRate", 99.7),
                Map.of("serviceName", "futures-market", "displayName", "行情服务", "instances", 2,
                        "healthy", 2, "status", "RUNNING", "qps", 5200, "avgResponseTime", 3, "successRate", 99.9),
                Map.of("serviceName", "futures-settlement", "displayName", "清结算服务", "instances", 1,
                        "healthy", 1, "status", "RUNNING", "qps", 5, "avgResponseTime", 350, "successRate", 99.3)
        );
        return services;
    }

    private List<Map<String, Object>> buildMockInstances(String serviceName) {
        List<Map<String, Object>> instances = new ArrayList<>();
        for (int i = 1; i <= (serviceName.contains("order") ? 3 : 2); i++) {
            Map<String, Object> inst = new LinkedHashMap<>();
            inst.put("instanceId", serviceName + "-" + i);
            inst.put("ip", "192.168.1." + (10 + i));
            inst.put("port", 8080 + i);
            inst.put("status", i == 2 && serviceName.contains("account") ? "DOWN" : "UP");
            inst.put("healthCheck", i == 2 && serviceName.contains("account") ? "FAILED" : "PASSED");
            inst.put("uptime", (int)(24 * 7 * i + Math.random() * 24) + "h");
            inst.put("cpuUsage", BigDecimal.valueOf(35 + Math.random() * 40).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            inst.put("memoryUsage", BigDecimal.valueOf(50 + Math.random() * 30).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            inst.put("startTime", LocalDateTime.now().minusDays(7 * i).format(DTF));
            instances.add(inst);
        }
        return instances;
    }

    private Map<String, Object> getMockServiceDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalServices", 8);
        data.put("healthyServices", 7);
        data.put("unhealthyServices", 1);
        data.put("totalInstances", 16);
        data.put("overallSuccessRate", 99.2);
        data.put("avgResponseTime", 45);
        data.put("services", buildMockServices());
        data.put("dependencies", List.of(
                Map.of("from", "futures-gateway", "to", "futures-order"),
                Map.of("from", "futures-gateway", "to", "futures-account"),
                Map.of("from", "futures-gateway", "to", "futures-market"),
                Map.of("from", "futures-order", "to", "futures-matching"),
                Map.of("from", "futures-order", "to", "futures-fund"),
                Map.of("from", "futures-order", "to", "futures-risk"),
                Map.of("from", "futures-fund", "to", "futures-settlement"),
                Map.of("from", "futures-matching", "to", "futures-market")
        ));
        return data;
    }

    private List<Map<String, Object>> buildMockReleaseHistory(int page) {
        List<Map<String, Object>> releases = new ArrayList<>();
        String[] services = {"futures-order", "futures-matching", "futures-gateway", "futures-fund", "futures-risk"};
        String[] statuses = {"SUCCESS", "SUCCESS", "SUCCESS", "ROLLED_BACK", "IN_PROGRESS", "PENDING_APPROVAL"};
        String[] operators = {"张三", "李四", "王五"};
        String[] versions = {"2.1.0", "2.0.5", "2.0.4", "2.0.3", "2.0.2", "2.0.1"};

        for (int i = 1; i <= 10; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("releaseId", "RL-" + String.format("%08d", i));
            r.put("serviceName", services[i % services.length]);
            r.put("version", versions[i % versions.length]);
            r.put("status", statuses[i % statuses.length]);
            r.put("operator", operators[i % operators.length]);
            r.put("approver", i % 3 == 0 ? "赵审核" : "-");
            r.put("strategy", i % 2 == 0 ? "BLUE_GREEN" : "GRAY_RELEASE");
            r.put("grayPercent", i % 3 == 0 ? 50 : (i % 2 == 0 ? 20 : 100));
            r.put("description", services[i % services.length].replace("futures-", "") + " 服务版本更新");
            r.put("rollbackVersion", i % 3 == 0 ? versions[(i + 1) % versions.length] : "");
            r.put("createTime", LocalDateTime.now().minusDays(i).format(DTF));
            r.put("deployTime", i % 4 == 0 ? "" : LocalDateTime.now().minusDays(i).plusHours(2).format(DTF));
            r.put("finishTime", i % 4 == 0 ? "" : LocalDateTime.now().minusDays(i).plusHours(3).format(DTF));
            releases.add(r);
        }
        return releases;
    }

    private Map<String, Object> buildMockReleaseDetail(String releaseId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("releaseId", releaseId);
        detail.put("serviceName", "futures-order");
        detail.put("version", "2.1.0");
        detail.put("previousVersion", "2.0.5");
        detail.put("status", "IN_PROGRESS");
        detail.put("operator", "张三");
        detail.put("approver", "李四");
        detail.put("strategy", "GRAY_RELEASE");
        detail.put("grayPercent", 20);
        detail.put("description", "订单服务灰度发布，增加新撮合算法");
        detail.put("changelog", "- 优化限价单撮合性能\n- 修复撤单并发bug\n- 增加止损单支持");
        detail.put("createTime", LocalDateTime.now().minusHours(5).format(DTF));
        detail.put("approveTime", LocalDateTime.now().minusHours(4).format(DTF));
        detail.put("deployTime", LocalDateTime.now().minusHours(1).format(DTF));
        detail.put("instances", List.of(
                Map.of("instanceId", "order-1", "status", "UPDATED", "version", "2.1.0"),
                Map.of("instanceId", "order-2", "status", "UPDATED", "version", "2.1.0"),
                Map.of("instanceId", "order-3", "status", "PENDING", "version", "2.0.5")
        ));
        return detail;
    }

    private Map<String, Object> getMockReleaseStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReleases", 156);
        stats.put("successReleases", 142);
        stats.put("failedReleases", 8);
        stats.put("rolledBackReleases", 6);
        stats.put("pendingApprovals", 3);
        stats.put("inProgressReleases", 1);
        stats.put("successRate", 91.0);

        // 近7天发布趋势
        stats.put("dailyReleases", List.of(
                Map.of("date", "07-06", "total", 3, "success", 3),
                Map.of("date", "07-07", "total", 5, "success", 4),
                Map.of("date", "07-08", "total", 2, "success", 2),
                Map.of("date", "07-09", "total", 4, "success", 3),
                Map.of("date", "07-10", "total", 6, "success", 5),
                Map.of("date", "07-11", "total", 3, "success", 3),
                Map.of("date", "07-12", "total", 1, "success", 0)
        ));
        return stats;
    }

    private List<Map<String, Object>> buildMockConfigs(String serviceName, String env) {
        String targetService = (serviceName == null || serviceName.isEmpty()) ? "futures-order" : serviceName;
        String targetEnv = (env == null || env.isEmpty()) ? "dev" : env;
        String prefix = "[" + targetEnv + "] ";

        List<Map<String, Object>> configs = List.of(
                Map.of("configId", "cfg-" + targetService + "-01", "configName", prefix + "数据库连接URL",
                        "configKey", "spring.datasource.url", "configValue", "jdbc:mysql://localhost:3306/futures_" + targetService +
                                "?useSSL=false&serverTimezone=Asia/Shanghai",
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-07-10 14:30:00", "version", 12),
                Map.of("configId", "cfg-" + targetService + "-02", "configName", prefix + "数据库用户名",
                        "configKey", "spring.datasource.username", "configValue", "futures_" + targetService,
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-07-01 09:00:00", "version", 2),
                Map.of("configId", "cfg-" + targetService + "-03", "configName", prefix + "Redis连接地址",
                        "configKey", "spring.redis.host", "configValue", "redis-master:6379",
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-06-15 11:20:00", "version", 5),
                Map.of("configId", "cfg-" + targetService + "-04", "configName", prefix + "日志级别",
                        "configKey", "logging.level.root", "configValue", targetEnv.equals("prod") ? "INFO" : "DEBUG",
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-07-05 16:45:00", "version", 8),
                Map.of("configId", "cfg-" + targetService + "-05", "configName", prefix + "服务端口",
                        "configKey", "server.port", "configValue", String.valueOf(8080 + Arrays.asList(
                                "futures-gateway","futures-order","futures-matching","futures-account",
                                "futures-fund","futures-risk","futures-market","futures-settlement"
                        ).indexOf(targetService)),
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-06-01 08:00:00", "version", 1),
                Map.of("configId", "cfg-" + targetService + "-06", "configName", prefix + "连接池最大连接数",
                        "configKey", "spring.datasource.hikari.maximum-pool-size", "configValue",
                        targetEnv.equals("prod") ? "30" : "10",
                        "env", targetEnv, "serviceName", targetService, "lastModified", "2024-07-08 10:30:00", "version", 6)
        );
        return configs;
    }

    private Map<String, Object> buildMockConfigDetail(String configId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("configId", configId);
        detail.put("configName", "数据库连接URL");
        detail.put("configKey", "spring.datasource.url");
        detail.put("configValue", "jdbc:mysql://localhost:3306/futures_order?useSSL=false&serverTimezone=Asia/Shanghai");
        detail.put("env", "dev");
        detail.put("serviceName", "futures-order");
        detail.put("description", "订单服务数据库连接配置");
        detail.put("encrypted", false);
        detail.put("lastModified", "2024-07-10 14:30:00");
        detail.put("lastModifier", "张三");
        detail.put("version", 12);
        return detail;
    }

    private List<Map<String, Object>> buildMockConfigChanges(String configId) {
        List<Map<String, Object>> changes = List.of(
                Map.of("changeId", "chg-001", "version", 12, "operator", "张三", "oldValue",
                        "jdbc:mysql://old-host:3306/futures_order", "newValue",
                        "jdbc:mysql://localhost:3306/futures_order", "changeTime", "2024-07-10 14:30:00",
                        "status", "APPLIED", "reason", "数据库迁移"),
                Map.of("changeId", "chg-002", "version", 11, "operator", "李四", "oldValue",
                        "jdbc:mysql://old-host:3306/futures_order?useSSL=true", "newValue",
                        "jdbc:mysql://old-host:3306/futures_order?useSSL=false", "changeTime", "2024-07-08 11:00:00",
                        "status", "APPLIED", "reason", "关闭SSL加速连接"),
                Map.of("changeId", "chg-003", "version", 10, "operator", "王五", "oldValue",
                        "jdbc:mysql://old-host:3306/futures_order_v1", "newValue",
                        "jdbc:mysql://old-host:3306/futures_order?useSSL=true", "changeTime", "2024-06-20 09:15:00",
                        "status", "ROLLED_BACK", "reason", "数据库名变更回滚")
        );
        return changes;
    }

    private List<Map<String, Object>> buildMockConfigDiff(String serviceName, String envA, String envB) {
        String[][] diffs = {
                {"spring.datasource.url", envA + ": jdbc:mysql://localhost:3306/" + serviceName,
                        envB + ": jdbc:mysql://prod-db:3306/" + serviceName, "差异"},
                {"logging.level.root", envA + ": DEBUG", envB + ": INFO", "差异"},
                {"spring.datasource.hikari.maximum-pool-size", envA + ": 10", envB + ": 30", "差异"},
                {"server.port", envA + ": 8081", envB + ": 8081", "相同"},
                {"spring.redis.host", envA + ": redis-master:6379", envB + ": redis-cluster:6379", "差异"}
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] d : diffs) {
            result.add(Map.of("configKey", d[0], "envAValue", d[1], "envBValue", d[2], "status", d[3]));
        }
        return result;
    }

    private List<Map<String, Object>> buildMockLogs(String serviceName, String level,
                                                    String keyword, String traceId, int page) {
        String targetService = (serviceName == null || serviceName.isEmpty()) ? "futures-order" : serviceName;
        String[] levels = {"INFO", "INFO", "INFO", "WARN", "ERROR", "INFO", "INFO", "WARN", "INFO", "INFO"};
        String[] messages = {
                "订单 [ORD-20240712001] 创建成功，合约: HSI2309，方向: BUY，数量: 2",
                "资金冻结成功，订单ID: ORD-20240712001，冻结金额: 12500.00",
                "风控校验通过，用户ID: 1001，风险度: 45.3%",
                "订单 [ORD-20240712002] 撮合延迟警告，等待时间: 350ms",
                "撮合引擎连接超时，重试次数: 3/3，触发熔断",
                "成交回报处理成功，订单ID: ORD-20240712001，成交价: 18450",
                "用户 [1002] 持仓变动通知，HSI2309: 多+1手",
                "资金账户 [1005] 可用余额低于预警阈值，当前: 5234.00",
                "数据库连接池耗尽，max-active=20, active=20, idle=0",
                "WebSocket推送成功，目标: 终端-1001，消息类型: MARKET_TICK"
        };

        List<Map<String, Object>> logs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("logId", "log-" + serviceName + "-" + i);
            log.put("serviceName", targetService);
            log.put("instanceId", targetService + "-" + (i % 3 + 1));
            log.put("timestamp", LocalDateTime.now().minusMinutes((page - 1) * 10 + i).format(DTF));
            log.put("level", levels[i - 1]);
            log.put("logger", "com.futures." + targetService.replace("futures-", "") + ".service");
            log.put("thread", "http-nio-808" + (i % 3) + "-exec-" + i);
            log.put("traceId", traceId != null ? traceId : "trace-" + String.format("%016d", i * 1000 + page));
            log.put("message", keyword != null ? "【包含搜索关键字】" + messages[i - 1] : messages[i - 1]);
            logs.add(log);
        }
        return logs;
    }

    private List<Map<String, Object>> buildMockLogContext(String traceId) {
        List<Map<String, Object>> context = new ArrayList<>();
        String[] stages = {"API网关 收到请求", "订单服务 创建订单", "资金服务 冻结保证金",
                "风控服务 前置风控校验", "撮合引擎 撮合处理", "订单服务 更新订单状态"};
        for (int i = 0; i < stages.length; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", LocalDateTime.now().minusNanos((5000L - i * 800L) * 1000000L).format(DTF));
            entry.put("serviceName", stages[i].split(" ")[0]);
            entry.put("level", i == 4 ? "INFO" : "INFO");
            entry.put("message", stages[i] + " - traceId: " + traceId + " - 处理完成");
            context.add(entry);
        }
        return context;
    }

    private Map<String, Object> getMockLogStats(String serviceName) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLogs", 1256800);
        stats.put("errorCount", 125);
        stats.put("warnCount", 2340);
        stats.put("errorRate", 0.01);
        stats.put("topErrors", List.of(
                Map.of("type", "数据库连接超时", "count", 45),
                Map.of("type", "WebSocket推送失败", "count", 32),
                Map.of("type", "MQ消息消费失败", "count", 28),
                Map.of("type", "撮合引擎超时", "count", 12),
                Map.of("type", "JVM OOM警告", "count", 8)
        ));
        // 近24小时日志趋势
        List<Map<String, Object>> hourlyTrend = new ArrayList<>();
        for (int i = 23; i >= 0; i--) {
            hourlyTrend.add(Map.of(
                    "hour", String.format("%02d:00", LocalDateTime.now().minusHours(i).getHour()),
                    "total", (int)(500 + Math.random() * 1000),
                    "error", (int)(Math.random() * 5),
                    "warn", (int)(Math.random() * 20)
            ));
        }
        stats.put("hourlyTrend", hourlyTrend);
        return stats;
    }

    private List<Map<String, Object>> buildMockAlerts(String level, String status,
                                                      String serviceName, int page) {
        String[] levels = {"CRITICAL", "WARNING", "WARNING", "CRITICAL", "INFO", "WARNING", "CRITICAL", "INFO", "WARNING", "WARNING"};
        String[] services = {"futures-order", "futures-matching", "futures-gateway", "futures-account",
                "futures-fund", "futures-risk", "futures-market", "futures-settlement",
                "futures-order", "futures-matching"};
        String[] messages = {
                "撮合引擎响应时间P99超过100ms阈值，当前: 156ms",
                "订单服务错误率超过5%阈值，当前: 7.2%",
                "API网关QPS突增，当前: 5200/s，基线: 2000/s",
                "账户服务实例 [account-2] 健康检查失败",
                "资金服务数据库连接池使用率超过85%",
                "用户 [1001] 风险度达到95.3%，接近强平阈值",
                "行情数据延迟超过500ms，当前: 2.3s",
                "清结算任务执行超时，耗时: 12分钟",
                "MQ消息积压超过10000条，Topic: order-matched",
                "内存使用率超过85%，实例: matching-1"
        };

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("alertId", "alert-" + String.format("%04d", i));
            alert.put("alertName", services[i - 1].replace("futures-", "") + " 异常告警");
            alert.put("level", levels[i - 1]);
            alert.put("serviceName", services[i - 1]);
            alert.put("message", messages[i - 1]);
            alert.put("status", i <= 3 ? "TRIGGERED" : (i <= 6 ? "ACKNOWLEDGED" : "RESOLVED"));
            alert.put("claimedBy", i <= 3 ? "-" : (i <= 6 ? "张三" : "李四"));
            alert.put("claimedAt", i <= 3 ? "" : LocalDateTime.now().minusHours(i).format(DTF));
            alert.put("resolvedAt", i <= 6 ? "" : LocalDateTime.now().minusHours(i - 3).format(DTF));
            alert.put("resolution", i <= 6 ? "" : "已处理完成");
            alert.put("triggerTime", LocalDateTime.now().minusHours(i + 1).format(DTF));
            alert.put("duration", i * 10 + "分钟");
            alerts.add(alert);
        }
        return alerts;
    }

    private Map<String, Object> getMockAlertStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAlerts", 128);
        stats.put("triggeredAlerts", 5);
        stats.put("acknowledgedAlerts", 8);
        stats.put("resolvedAlerts", 115);
        stats.put("criticalAlerts", 12);
        stats.put("warningAlerts", 38);
        stats.put("infoAlerts", 78);
        stats.put("avgResolutionTime", 25.5);

        // 近7天告警趋势
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        String[] dates = {"07-06", "07-07", "07-08", "07-09", "07-10", "07-11", "07-12"};
        for (int i = 0; i < dates.length; i++) {
            dailyTrend.add(Map.of(
                    "date", dates[i],
                    "critical", (int)(Math.random() * 3),
                    "warning", (int)(3 + Math.random() * 6),
                    "info", (int)(5 + Math.random() * 10)
            ));
        }
        stats.put("dailyTrend", dailyTrend);

        // 按服务分布
        stats.put("byService", List.of(
                Map.of("service", "futures-order", "count", 32),
                Map.of("service", "futures-matching", "count", 18),
                Map.of("service", "futures-gateway", "count", 28),
                Map.of("service", "futures-account", "count", 12),
                Map.of("service", "futures-fund", "count", 8),
                Map.of("service", "futures-risk", "count", 15),
                Map.of("service", "futures-market", "count", 10),
                Map.of("service", "futures-settlement", "count", 5)
        ));

        return stats;
    }

    private List<Map<String, Object>> buildMockAuditLogs(String operator, String action,
                                                         String module, int page) {
        String[] modules = {"服务发布", "配置变更", "扩缩容", "服务发布", "配置变更", "配置变更", "服务发布", "扩缩容", "服务发布", "配置变更"};
        String[] actions = {"发布", "修改", "扩容", "回滚", "新增", "删除", "审批", "缩容", "创建发布单", "批量修改"};
        String[] operators = {"张三", "李四", "王五", "张三", "赵审核", "李四", "赵审核", "王五", "张三", "李四"};
        String[] detail = {
                "发布 futures-order 2.1.0 → 2.2.0（灰度50%）",
                "修改 futures-order 数据库连接URL",
                "futures-matching 实例数 2 → 3",
                "回滚 futures-gateway 2.0.5 → 2.0.4",
                "新增 futures-market 数据库连接配置",
                "删除 futures-account 过期配置项",
                "审批发布单 RL-00000001，批准",
                "futures-order 实例数 3 → 2",
                "创建发布单：futures-risk 2.0.1 → 2.0.2",
                "批量修改 futures-order 连接池配置"
        };

        List<Map<String, Object>> logs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("auditId", "audit-" + String.format("%06d", (page - 1) * 10 + i));
            log.put("operator", operators[i - 1]);
            log.put("module", modules[i - 1]);
            log.put("action", actions[i - 1]);
            log.put("detail", detail[i - 1]);
            log.put("ip", "192.168.1." + (100 + i));
            log.put("userAgent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
            log.put("result", i == 7 ? "PENDING" : "SUCCESS");
            log.put("operateTime", LocalDateTime.now().minusMinutes(i * 15).format(DTF));
            log.put("duration", (int)(100 + Math.random() * 900) + "ms");
            logs.add(log);
        }
        return logs;
    }
}
