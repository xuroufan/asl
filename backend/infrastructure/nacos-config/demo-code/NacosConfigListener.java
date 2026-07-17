package com.futures.account.listener;

import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Nacos 配置变更监听器
 *
 * 方式一：使用 @NacosConfigListener 注解（推荐）
 * 方式二：实现 Listener 接口（程序化注册）
 *
 * 当 Nacos 中的配置文件发生变更时，这些监听器会自动触发。
 * 可用于：
 * - 配置变更通知（日志、推送）
 * - 动态调整线程池
 * - 动态切换数据源
 * - 动态调整限流阈值
 */
@Slf4j
@Component
public class NacosConfigListener {

    /**
     * 监听服务级别配置变更
     * dataId = "futures-account-dev.yaml" 上的任何变更都会触发此方法
     */
    @NacosConfigListener(
        dataId = "futures-account-dev.yaml",
        groupId = "DEFAULT_GROUP",
        timeout = 500
    )
    public void onServiceConfigChange(String newConfig) {
        log.info("============================================");
        log.info("[配置变更] futures-account 配置已更新");
        log.info("[配置变更] 新配置内容预览 (前200字符)");
        log.info("--------------------------------------------");
        if (newConfig != null && newConfig.length() > 200) {
            log.info(newConfig.substring(0, 200) + "...");
        } else {
            log.info(newConfig);
        }
        log.info("============================================");

        // 在此处执行配置变更后的自定义逻辑：
        // - 通知运维人员（钉钉/企微）
        // - 记录审计日志
        // - 调整业务参数
    }

    /**
     * 监听全局共享配置变更
     */
    @NacosConfigListener(
        dataId = "futures-shared-dev.yaml",
        groupId = "GLOBAL_GROUP",
        timeout = 500
    )
    public void onGlobalConfigChange(String newConfig) {
        log.info("[全局配置变更] 共享配置已更新");
    }

    /**
     * 方式二：程序化注册 Listener
     * 适用于需要动态控制监听行为的场景
     */
    public static class CustomConfigListener implements Listener {

        private final String dataId;
        private final String group;

        public CustomConfigListener(String dataId, String group) {
            this.dataId = dataId;
            this.group = group;
        }

        @Override
        public void receiveConfigInfo(String configInfo) {
            log.info("Listener 收到配置变更: dataId={}, group={}", dataId, group);
            // 自定义配置处理逻辑
        }

        @Override
        public Executor getExecutor() {
            // 返回 null 表示使用 Nacos 的默认线程池
            return null;
        }
    }
}
