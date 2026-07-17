# 期货交易平台 - 基础设施资源规格

## 1. 开发/测试环境 (Docker Compose)

| 组件 | CPU (核) | 内存 | 存储 | 实例数 |
|------|---------|------|------|--------|
| MySQL Master | 1.0 | 1G | 10G | 1 |
| MySQL Slave | 0.5 | 1G | 10G | 1 |
| Redis Master | 0.5 | 512M | 1G | 1 |
| Nacos | 0.5 | 1G | 1G | 1 |
| Seata Server | 0.5 | 512M | 500M | 1 |
| RocketMQ Namesrv | 0.25 | 512M | 500M | 1 |
| RocketMQ Broker | 0.5 | 1G | 2G | 1 |
| Elasticsearch | 0.5 | 1G | 5G | 1 |
| SkyWalking OAP | 0.5 | 1G | 500M | 1 |
| Prometheus | 0.25 | 512M | 5G | 1 |
| Grafana | 0.25 | 256M | 1G | 1 |
| **合计** | **~5.25** | **~8.3G** | **~36G** | **11** |

> 每个微服务 Java 进程额外需要 256M-512M 内存
> MacBook 建议总内存预留: 8-12G

## 2. 生产环境 (Kubernetes)

### 2.1 中间件资源规格

| 组件 | 请求 CPU | 请求内存 | 限制 CPU | 限制内存 | 存储 | 副本数 | 推荐节点类型 |
|------|---------|---------|---------|---------|------|--------|------------|
| Nacos | 0.5 | 512M | 1.0 | 1G | 10G | 3 | 通用型 |
| MySQL | 1.0 | 1G | 2.0 | 4G | 100G+SSD | 1主2从 | 高IO型 |
| Redis | 0.5 | 512M | 1.0 | 1G | 20G | 3哨兵+1主2从 | 内存型 |
| RocketMQ NS | 0.5 | 512M | 1.0 | 1G | - | 2 | 通用型 |
| RocketMQ Broker | 1.0 | 2G | 2.0 | 4G | 50G+SSD | 3 | 高IO型 |
| Kafka | 1.0 | 2G | 2.0 | 4G | 100G+SSD | 3 | 高IO型 |
| Seata | 0.5 | 512M | 1.0 | 1G | 10G | 2 | 通用型 |
| ES | 1.0 | 2G | 2.0 | 4G | 100G+SSD | 3 | 高IO型 |
| SkyWalking OAP | 0.5 | 1G | 1.0 | 2G | - | 2 | 通用型 |
| Prometheus | 0.5 | 1G | 1.0 | 2G | 50G | 1 | 通用型 |
| Grafana | 0.25 | 256M | 0.5 | 512M | 10G | 1 | 通用型 |

### 2.2 微服务资源规格

| 服务 | 请求 CPU | 请求内存 | 限制 CPU | 限制内存 | 副本数 | 备注 |
|------|---------|---------|---------|---------|--------|------|
| Gateway | 0.5 | 512M | 1.0 | 1G | 2 | 无状态, 可水平扩 |
| Account | 0.5 | 512M | 1.0 | 1G | 2 | 无状态 |
| Order | 0.5 | 512M | 1.0 | 1G | 3 | 高并发 |
| Matching | 1.0 | 1G | 2.0 | 2G | 3 | CPU密集, 内存撮合 |
| Fund | 0.5 | 512M | 1.0 | 1G | 2 | 无状态 |
| Risk | 0.5 | 512M | 1.0 | 1G | 2 | 无状态 |
| Market | 1.0 | 1G | 2.0 | 2G | 2 | IO密集 |
| Settlement | 0.5 | 512M | 1.0 | 1G | 1 | 定时任务 |

### 2.3 生产环境集群预估

| 项目 | 预估 |
|------|------|
| Master 节点 | 3台 (8C/16G) |
| Worker 节点 | 4-6台 (16C/32G) |
| 总 CPU | 88-120 核 |
| 总内存 | 176-256 GB |
| 存储 | SSD 1-2 TB (数据库/日志) |
| 带宽 | 1-10 Gbps |

## 3. JVM 参数建议

```bash
# 通用服务 (order/account/fund/risk/settlement)
JAVA_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 高负载服务 (matching/market/gateway)  
JAVA_OPTS="-server -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=50"

# Gateway (响应时间敏感)
JAVA_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC -Dspring.classformat.ignore=true"
```
