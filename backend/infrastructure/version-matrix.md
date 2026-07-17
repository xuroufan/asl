# 期货交易平台 - 版本兼容性矩阵

## 1. 核心框架版本 (pom.xml)

| 框架 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Spring Cloud | 2023.0.1 | 微服务治理 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里云组件集成 |
| Java | 21+ (兼容 26) | JDK 编译版本 |
| Maven | 3.8+ | 项目构建工具 |

## 2. 中间件版本矩阵

| 组件 | 推荐版本 | 生产版本 | 用途 | 兼容性说明 |
|------|---------|---------|------|-----------|
| Nacos | 2.3.2 | 2.4.x | 服务注册发现 + 配置中心 | SCA 2023.0.x -> Nacos 2.3.x |
| Seata | 1.8.0 | 1.7.1+ | 分布式事务 (AT+TCC) | SCA 2023.0.x -> Seata 1.8.x |
| RocketMQ | 5.2.0 | 5.x | 消息队列 (核心用) | SCA 2023.0.x -> RocketMQ 5.x |
| Kafka | 3.4+ | 3.4+ | 消息队列 (备选/行情用) | 独立组件, 无版本耦合 |
| Sentinel | 1.8.7 | 1.8.7+ | 熔断限流降级 | SCA 2023.0.x -> Sentinel 1.8.x |
| SkyWalking | 9.4.0 | 9.4.0+ | 分布式链路追踪 | ES 存储需版本匹配 |

## 3. 数据层版本

| 组件 | 开发版本 | 生产版本 | 用途 |
|------|---------|---------|------|
| MySQL | 8.0 | 8.0+ | 业务数据持久化 |
| Redis | 7-alpine | 7.x | 缓存 + 分布式锁 |
| Elasticsearch | 7.14.0 | 7.14+ | SkyWalking 存储 |

## 4. 监控层版本

| 组件 | 版本 | 用途 |
|------|------|------|
| Prometheus | 2.40+ | 指标采集 |
| Grafana | 9.2+ | 可视化监控仪表盘 |

## 5. Spring Cloud Alibaba 版本对应关系

| Spring Cloud Alibaba | Spring Cloud | Spring Boot | Nacos | Seata | Sentinel |
|---------------------|-------------|-------------|-------|-------|----------|
| 2023.0.1.0 | 2023.0.x | 3.2.x | 2.3.2 | 1.8.0 | 1.8.7 |
| 2022.0.0.0 | 2022.0.x | 3.1.x | 2.2.3 | 1.7.1 | 1.8.6 |
| 2021.0.5.0 | 2021.0.x | 2.7.x | 2.1.2 | 1.5.2 | 1.8.4 |

> **注意**：当前项目使用 Spring Boot 3.2.5 + SCA 2023.0.1.0，这是最新的兼容组合之一。
> Java 26 运行需添加 `-Dspring.classformat.ignore=true` JVM 参数。

## 6. 依赖版本检查清单

```
spring-boot:                 3.2.5
spring-cloud:                2023.0.1
spring-cloud-alibaba:        2023.0.1.0
nacos-client:                2.3.2
seata:                       1.8.0
rocketmq:                    5.2.0
mybatis-plus:                3.5.6
mysql-connector-j:           8.4.0
druid:                       1.2.22
redisson:                    3.30.0
jjwt:                        0.12.5
hutool:                      5.8.28
knife4j:                     4.5.0
```
