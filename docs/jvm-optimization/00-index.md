# JVM 与内存稳定性优化方案

**期货交易平台 · 后端微服务**
> 适用 JDK 21 + Spring Boot 3.x + Spring Cloud 微服务架构

---

## 微服务清单与规格

| 服务 | 端口 | 容器内存 | 堆内存 | CPU | 数据库 | DB 连接池 | 关键特性 |
|------|------|---------|--------|-----|--------|----------|---------|
| `futures-gateway` | 8080 | 768Mi | 512m | 2 | 无 | 无 | Spring Cloud Gateway, Netty |
| `futures-order` | 8081 | 1Gi | 768m | 2 | MySQL | **Druid→HikariCP** | 订单CRUD, RocketMQ |
| `futures-matching` | 8082 | **2Gi** | **1.5g** | **4** | 无 | 无 | Disruptor, 内存撮合 |
| `futures-account` | 8083 | 1Gi | 768m | 2 | MySQL | HikariCP | 账户管理 |
| `futures-fund` | 8084 | 768Mi | 512m | 2 | MySQL | HikariCP | 资金流水 |
| `futures-risk` | 8085 | 1Gi | 768m | 2 | 无 | 无 | Redis风控规则 |
| `futures-market` | 8086 | 1Gi | 768m | 2 | MySQL | HikariCP | K线聚合 |
| `futures-settlement` | 8087 | 1Gi | 768m | 2 | MySQL | HikariCP | 日终清算 |
| `futures-push` | 8088 | 512Mi | 256m | 1 | 无 | 无 | WebSocket推送 |
| `futures-admin` | 8089 | 768Mi | 512m | 1 | MySQL | HikariCP | 后台管理 |

---

## 目录

1. [JVM 参数标准化](./01-jvm-params.md)
2. [内存泄漏排查与修复](./02-memory-leak.md)
3. [连接池与线程池调优](./03-pool-tuning.md)
4. [堆外内存监控](./04-off-heap.md)
5. [GC 日志分析脚本](./05-gc-analysis.md)
6. [OOM 堆转储分析](./06-oom-analysis.md)
