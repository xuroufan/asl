# 日志规范与全链路 TraceId

> 格式: JSON 结构化日志 | TraceId 全链路传递 | 敏感数据脱敏

---

## 1. Logback JSON 日志配置

### 1.1 logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- ===== 全局属性 ===== -->
    <springProperty name="APP_NAME" source="spring.application.name" defaultValue="unknown"/>
    <property name="LOG_DIR" value="${LOG_DIR:-/var/log}"/>
    <property name="MAX_FILE_SIZE" value="500MB"/>
    <property name="MAX_HISTORY" value="30"/>

    <!-- ===== 1. JSON 格式输出（生产主输出） ===== -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${APP_NAME}.json.log</file>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${APP_NAME}.json.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            <maxHistory>${MAX_HISTORY}</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>

        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- 结构化字段 -->
            <includeCallerData>false</includeCallerData>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <thread>thread</thread>
                <message>message</message>
                <mdc>mdc</mdc>
            </fieldNames>

            <!-- 自定义全局字段 -->
            <customFields>{"service":"${APP_NAME}","environment":"${ENV:production}"}</customFields>
        </encoder>
    </appender>

    <!-- ===== 2. 控制台彩色输出（开发环境） ===== -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ===== 3. 错误日志单独文件 ===== -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${APP_NAME}-error.json.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>WARN</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.json.log</fileNamePattern>
            <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            <maxHistory>${MAX_HISTORY}</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <!-- ===== 4. 异步写入（提升性能） ===== -->
    <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="JSON_FILE"/>
        <neverBlock>true</neverBlock>           <!-- 队列满时不阻塞业务 -->
        <queueSize>2048</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <!-- ===== 根配置 ===== -->
    <root level="INFO">
        <springProfile name="dev,test">
            <appender-ref ref="CONSOLE"/>
        </springProfile>
        <springProfile name="prod">
            <appender-ref ref="ASYNC_JSON"/>
            <appender-ref ref="ERROR_FILE"/>
        </springProfile>
    </root>
</configuration>
```

### 1.2 JSON 日志输出示例

```json
{
    "timestamp": "2026-07-17T10:00:00.123Z",
    "level": "INFO",
    "service": "order-service",
    "logger": "com.hackfuture.order.service.OrderService",
    "thread": "http-nio-8080-exec-3",
    "message": "订单创建成功: orderId=ORD-123456",
    "mdc": {
        "traceId": "tkb8a1f2e3c4d5",
        "userId": "U12345",
        "requestId": "req-abc-123-xyz"
    },
    "duration_ms": 45,
    "extra": {
        "symbol": "HSI2309",
        "volume": 2,
        "orderId": "ORD-123456"
    },
    "environment": "production"
}
```

---

## 2. MDC TraceId 全链路传递

### 2.1 请求进入时生成 TraceId（Gateway Filter）

```java
package com.hackfuture.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gateway 统一 TraceId 生成
 */
@Slf4j
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders()
                .getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        String userId = exchange.getRequest().getHeaders()
                .getFirst(USER_ID_HEADER);

        // 注入 MDC
        MDC.put("traceId", traceId);
        if (userId != null) MDC.put("userId", userId);

        // 传递到下游服务
        exchange = exchange.mutate()
                .request(r -> r.header(TRACE_ID_HEADER, traceId))
                .build();

        return chain.filter(exchange)
                .doFinally(signalType -> MDC.clear());
    }

    private String generateTraceId() {
        return "tkb" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // 最先执行
    }
}
```

### 2.2 Web 应用 Filter（微服务中传递 TraceId）

```java
package com.hackfuture.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 微服务中接收上游传递的 TraceId，写入 MDC
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletResponse;

        try {
            // 从请求头获取 TraceId（Gateway 传递下来的）
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = "tkb" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            }
            MDC.put("traceId", traceId);

            String userId = request.getHeader(USER_ID_HEADER);
            if (userId != null) MDC.put("userId", userId);

            // 传递到响应头
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.setHeader(TRACE_ID_HEADER, traceId);

            chain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();  // 请求结束清理 MDC，防止线程复用导致 TraceId 混乱
        }
    }
}
```

### 2.3 Feign 拦截器传递 TraceId

```java
package com.hackfuture.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 请求拦截器：自动将当前 MDC 中的 TraceId 传递到下游服务
 */
@Configuration
public class FeignTraceIdInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return (RequestTemplate template) -> {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                template.header(TRACE_ID_HEADER, traceId);
            }
            String userId = MDC.get("userId");
            if (userId != null) {
                template.header(USER_ID_HEADER, userId);
            }
        };
    }
}
```

### 2.4 线程池装饰器（异步任务传递 TraceId）

```java
package com.hackfuture.common.concurrent;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;

/**
 * MDC 感知的线程池：提交任务时保留当前线程的 MDC 上下文
 */
public class MdcAwareThreadPoolExecutor extends ThreadPoolExecutor {

    public MdcAwareThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                       long keepAliveTime, TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new NamedThreadFactory(poolName));
    }

    @Override
    public void execute(Runnable command) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(() -> {
            try {
                MDC.setContextMap(context);  // 还原 MDC
                command.run();
            } finally {
                MDC.clear();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(() -> {
            try {
                MDC.setContextMap(context);
                return task.call();
            } finally {
                MDC.clear();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(() -> {
            try {
                MDC.setContextMap(context);
                task.run();
            } finally {
                MDC.clear();
            }
        }, result);
    }
}
```

---

## 3. 敏感数据脱敏

### 3.1 日志脱敏注解

```java
package com.hackfuture.common.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.Objects;

/**
 * 日志脱敏注解
 *
 * 用法:
 *   @Mask(type = MaskType.PASSWORD)
 *   private String password;
 *
 *   @Mask(type = MaskType.PHONE)
 *   private String phone;
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
public @interface Mask {
    MaskType type();
}

enum MaskType {
    PASSWORD,       // 全部掩码: ******
    PHONE,          // 138****1234
    ID_CARD,        // 110***********1234
    EMAIL,          // a***@example.com
    NAME,           // *三
    TOKEN           // eyJhb***_abc
}

/**
 * Jackson 序列化脱敏器
 */
class MaskSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private MaskType type;

    public MaskSerializer() {}

    public MaskSerializer(MaskType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider prov) throws IOException {
        gen.writeString(mask(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Mask annotation = property.getAnnotation(Mask.class);
        if (annotation != null) {
            return new MaskSerializer(annotation.type());
        }
        return this;
    }

    private static String mask(String value, MaskType type) {
        if (value == null || value.isEmpty()) return value;

        switch (type) {
            case PASSWORD:
                return "******";
            case PHONE:
                return value.length() >= 7
                        ? value.substring(0, 3) + "****" + value.substring(value.length() - 4)
                        : value;
            case ID_CARD:
                return value.length() >= 10
                        ? value.substring(0, 3) + "********" + value.substring(value.length() - 4)
                        : value;
            case EMAIL:
                int at = value.indexOf('@');
                if (at > 1) {
                    return value.charAt(0) + "***" + value.substring(at);
                }
                return value;
            case NAME:
                return value.length() >= 2
                        ? "*" + value.substring(1)
                        : value;
            case TOKEN:
                return value.length() > 10
                        ? value.substring(0, 6) + "***" + value.substring(value.length() - 4)
                        : value;
            default:
                return value;
        }
    }
}
```

### 3.2 使用示例

```java
import com.hackfuture.common.mask.Mask;
import com.hackfuture.common.mask.MaskType;

public class LoginRequest {
    private String username;

    @Mask(type = MaskType.PASSWORD)
    private String password;        // 日志中输出 "******"
}

public class UserProfile {
    private String username;

    @Mask(type = MaskType.PHONE)
    private String phone;           // 日志中输出 "138****1234"

    @Mask(type = MaskType.ID_CARD)
    private String idCard;          // 日志中输出 "110***********1234"
}

// 在 ObjectMapper 中注册序列化器即可
// @Bean
// public ObjectMapper objectMapper() {
//     SimpleModule module = new SimpleModule();
//     module.addSerializer(String.class, new MaskSerializer());
//     return Jackson2ObjectMapperBuilder.json()
//             .modules(module)
//             .build();
// }
```

---

## 4. ELK 索引生命周期策略

```json
// Elasticsearch ILM 策略
PUT _ilm/policy/trading-logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "set_priority": {
            "priority": 0
          },
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "60d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

// 索引模板
PUT _index_template/trading-logs-template
{
  "index_patterns": ["trading-logs-*"],
  "template": {
    "settings": {
      "index.lifecycle.name": "trading-logs-policy",
      "index.number_of_shards": 3,
      "index.number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "level":     { "type": "keyword" },
        "service":   { "type": "keyword" },
        "traceId":   { "type": "keyword" },
        "userId":    { "type": "keyword" },
        "message":   { "type": "text" },
        "duration_ms": { "type": "long" },
        "extra":     { "type": "object", "enabled": true }
      }
    }
  }
}
```
