package com.futures.common.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 异步线程池 TraceId 装饰器
 *
 * 将父线程的 MDC 上下文（主要是 traceId）传递给子线程，
 * 确保 @Async 异步任务中日志能正确打印 traceId。
 *
 * 使用方式:
 *   ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *   executor.setTaskDecorator(new TraceTaskDecorator());
 */
@Component
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获父线程的 MDC 上下文
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 将父线程 MDC 设置到子线程
                if (parentMdc != null) {
                    MDC.setContextMap(parentMdc);
                }
                runnable.run();
            } finally {
                // 子线程执行完后清除，避免线程池复用导致上下文残留
                MDC.clear();
            }
        };
    }
}
