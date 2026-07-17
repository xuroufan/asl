# 4. 堆外内存监控

## 4.1 Prometheus 堆外内存指标

### 添加 Micrometer 自定义指标

```java
/**
 * 堆外内存监控 - 添加到 futures-common 模块
 */
@Configuration
public class OffHeapMemoryMetricConfig {

    @Bean
    public MeterBinder offHeapMemoryMetrics() {
        return registry -> {
            // 1. 直接内存 (Direct Buffer)
            Gauge.builder("jvm.memory.direct.buffer.total.capacity", this,
                    m -> getBufferPoolMetric("direct", BufferPoolMXBean::getTotalCapacity))
                .description("Direct buffer total capacity")
                .tag("area", "offheap")
                .register(registry);

            Gauge.builder("jvm.memory.direct.buffer.memory.used", this,
                    m -> getBufferPoolMetric("direct", BufferPoolMXBean::getMemoryUsed))
                .description("Direct buffer memory used")
                .tag("area", "offheap")
                .register(registry);

            Gauge.builder("jvm.memory.direct.buffer.count", this,
                    m -> getBufferPoolMetric("direct", BufferPoolMXBean::getCount))
                .description("Direct buffer count")
                .tag("area", "offheap")
                .register(registry);

            // 2. Mapped Buffer (文件映射)
            Gauge.builder("jvm.memory.mapped.buffer.total.capacity", this,
                    m -> getBufferPoolMetric("mapped", BufferPoolMXBean::getTotalCapacity))
                .description("Mapped buffer total capacity")
                .tag("area", "offheap")
                .register(registry);

            Gauge.builder("jvm.memory.mapped.buffer.memory.used", this,
                    m -> getBufferPoolMetric("mapped", BufferPoolMXBean::getMemoryUsed))
                .description("Mapped buffer memory used")
                .tag("area", "offheap")
                .register(registry);
        };
    }

    private long getBufferPoolMetric(String poolName,
                                     java.util.function.ToLongFunction<BufferPoolMXBean> func) {
        return ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
            .stream()
            .filter(pool -> poolName.equals(pool.getName()))
            .mapToLong(func)
            .findFirst()
            .orElse(0);
    }
}
```

## 4.2 Prometheus 告警规则

```yaml
# prometheus/alerts/offheap-alerts.yml
groups:
  - name: offheap-memory
    rules:
      - alert: DirectMemoryHigh
        expr: jvm_memory_direct_buffer_memory_used > 536870912  # 512MB
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Direct memory > 512MB on {{ $labels.instance }}"
          description: "Direct memory: {{ $value | humanize1024 }}"

      - alert: DirectMemoryCritical
        expr: jvm_memory_direct_buffer_memory_used > 1073741824  # 1GB
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Direct memory > 1GB on {{ $labels.instance }}"
          description: "Immediate investigation required. Current: {{ $value | humanize1024 }}"

      - alert: DirectBufferCountHigh
        expr: jvm_memory_direct_buffer_count > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Direct buffer count > 1000 on {{ $labels.instance }}"
```

## 4.3 Grafana 仪表板配置

```json
{
  "title": "Off-Heap Memory Dashboard",
  "panels": [
    {
      "title": "Direct Memory",
      "type": "timeseries",
      "targets": [
        {
          "expr": "jvm_memory_direct_buffer_memory_used{application=~\"$service\"}",
          "legendFormat": "{{instance}}"
        }
      ],
      "yaxis": { "format": "bytes" },
      "thresholds": [
        { "value": 536870912, "color": "orange" },
        { "value": 1073741824, "color": "red" }
      ]
    },
    {
      "title": "Direct Buffer Count",
      "type": "timeseries",
      "targets": [
        {
          "expr": "jvm_memory_direct_buffer_count{application=~\"$service\"}",
          "legendFormat": "{{instance}}"
        }
      ],
      "yaxis": { "format": "short" },
      "thresholds": [
        { "value": 1000, "color": "orange" }
      ]
    },
    {
      "title": "JVM Heap vs Direct Memory",
      "type": "timeseries",
      "targets": [
        { "expr": "jvm_memory_used_bytes{area=\"heap\", application=~\"$service\"}", "legendFormat": "Heap" },
        { "expr": "jvm_memory_direct_buffer_memory_used{application=~\"$service\"}", "legendFormat": "Direct" }
      ]
    }
  ]
}
```

## 4.4 Netty 直接内存监控（gateway / push 服务）

```java
// 添加到 futures-gateway 和 futures-push
@Configuration
@ConditionalOnClass(io.netty.buffer.ByteBufAllocator.class)
public class NettyDirectMemoryConfig {

    @Bean
    public MeterBinder nettyDirectMemoryMetrics() {
        return registry -> {
            io.netty.buffer.PooledByteBufAllocator allocator =
                io.netty.buffer.PooledByteBufAllocator.DEFAULT;

            Gauge.builder("netty.direct.memory.used", allocator,
                    a -> (double) a.metric().usedDirectMemory())
                .description("Netty Pooled Direct Memory Used")
                .register(registry);

            Gauge.builder("netty.heap.memory.used", allocator,
                    a -> (double) a.metric().usedHeapMemory())
                .description("Netty Pooled Heap Memory Used")
                .register(registry);
        };
    }
}
```

## 4.5 docker-compose 监控栈

```yaml
# infrastructure/docker-compose.observability.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:v2.53.0
    container_name: futures-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alerts:/etc/prometheus/alerts
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=30d'
      - '--storage.tsdb.retention.size=50GB'
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:11.2.0
    container_name: futures-grafana
    environment:
      GF_AUTH_ANONYMOUS_ENABLED: "true"
    volumes:
      - grafana_data:/var/lib/grafana
    ports: ["3000:3000"]

  alertmanager:
    image: prom/alertmanager:v0.27.0
    container_name: futures-alertmanager
    volumes:
      - ./prometheus/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    ports: ["9093:9093"]
```
