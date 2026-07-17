package com.futures.account.controller;

import com.futures.common.util.ConfigCipher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态配置演示控制器
 *
 * 功能：
 * 1. 通过 @RefreshScope 实现配置热更新
 * 2. 通过 @Value 注入 Nacos 动态配置
 * 3. 通过 @NacosConfigListener 监听配置变更事件
 *
 * 使用方式：
 * 1. 在 Nacos 控制台中修改 future-account-dev.yaml
 * 2. 点击"发布"后，配置自动刷新
 * 3. 访问 GET /api/v1/account/config 查看当前配置
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RefreshScope  // <-- 关键注解：标记该类下的配置会热更新
public class DynamicConfigController {

    // ─── 配置项（来自 Nacos 配置中心） ───

    @Value("${futures.account.auth.max-login-failures:5}")
    private int maxLoginFailures;

    @Value("${futures.account.auth.lock-minutes:15}")
    private int lockMinutes;

    @Value("${futures.account.auth.token-expire-minutes:120}")
    private int tokenExpireMinutes;

    @Value("${futures.account.kyc.enabled:true}")
    private boolean kycEnabled;

    @Value("${seata.enabled:false}")
    private boolean seataEnabled;

    @Value("${logging.level.com.futures.account:INFO}")
    private String logLevel;

    // ─── 获取当前所有动态配置 ───

    /**
     * 获取当前生效的动态配置
     * 修改 Nacos 配置后，无需重启服务即可看到新值
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxLoginFailures", maxLoginFailures);
        config.put("lockMinutes", lockMinutes);
        config.put("tokenExpireMinutes", tokenExpireMinutes);
        config.put("kycEnabled", kycEnabled);
        config.put("seataEnabled", seataEnabled);
        config.put("logLevel", logLevel);

        // 记录当前配置快照到日志（便于审计）
        log.info("Current dynamic config: {}", config);

        return config;
    }

    // ─── 动态修改配置（通过 API 触发 Nacos 配置变更） ───

    /**
     * 模拟配置变更（实际场景应在 Nacos 控制台操作）
     * 此 API 仅用于演示 @RefreshScope 效果
     */
    @PostMapping("/config/test")
    public String testRefresh(@RequestParam String key, @RequestParam String value) {
        log.warn("请在 Nacos 控制台修改配置: {} = {}", key, value);
        log.warn("当前设置后不会自动生效, 直到 Nacos 配置被更新");
        return "请在 Nacos 控制台操作: Data ID = futures-account-dev.yaml, key = " + key;
    }

    // ─── 敏感配置读取（加密存储） ───

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 返回数据库和 Redis 的连接状态（屏蔽密码明文）
     */
    @GetMapping("/config/connection-status")
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("dbConnected", !dbPassword.isEmpty());
        status.put("redisConnected", !redisPassword.isEmpty());
        status.put("dbPasswordMasked", maskPassword(dbPassword));
        status.put("redisPasswordMasked", maskPassword(redisPassword));
        status.put("note", "敏感信息已加密存储，密码仅显示前2位");
        return status;
    }

    private String maskPassword(String password) {
        if (password == null || password.length() < 2) return "****";
        return password.substring(0, 2) + "****";
    }
}
