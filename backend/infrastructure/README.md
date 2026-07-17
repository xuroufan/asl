# 期货交易平台 - 基础设施子模块

## 目录结构

```
infrastructure/
├── README.md                           # 本文件
├── version-matrix.md                   # 版本兼容性矩阵
├── architecture.md                     # 架构拓扑（含数据流向和端口分配）
├── resource-specs.md                   # 资源规格建议（开发/生产/容器/JVM）
├── nacos-cluster.md                    # Nacos 集群配置与服务注册指南
├── scripts/                            # 配置文件脚本
│   ├── mysql-master.cnf                # MySQL 主库配置
│   ├── mysql-slave.cnf                # MySQL 从库配置
│   ├── redis-sentinel.conf            # Redis 哨兵配置
│   ├── rocketmq-broker.conf           # RocketMQ 代理配置
│   ├── seata-registry.conf            # Seata 注册中心配置
│   ├── prometheus.yml                 # Prometheus 抓取配置
│   └── grafana-datasources.yml        # Grafana 数据源配置
└── k8s/                                # Kubernetes 部署骨架
    ├── namespace.yaml                  # 命名空间
    ├── deployment-template.yaml        # 通用部署模板
    ├── gateway-deployment.yaml         # Gateway 特定部署
    └── matching-deployment.yaml        # 撮合引擎部署（反亲和）
```

## 一键启动基础设施

```bash
# 启动所有中间件（MySQL, Redis, Nacos, RocketMQ, Kafka, Seata, ES, SkyWalking, Prometheus, Grafana）
cd /Users/fangfang/Documents/黑期/futures-platform
docker compose up -d

# 查看启动状态
docker compose ps

# 查看日志
docker compose logs -f nacos
docker compose logs -f skywalking-oap
```

## 服务注册接入 Nacos

让微服务使用 Nacos 服务发现：

```bash
# 1. 修改每个服务的 bootstrap.yml
#    启用 nacos.discovery.enabled=true
#    启用 nacos.config.enabled=true

# 2. 修改 Gateway 路由
#    将 uri: http://localhost:8081 改为 uri: lb://futures-order

# 3. 修改 Feign 客户端
#    移除 url= 硬编码，使用服务名
#    @FeignClient(name = "futures-order")

# 4. 启动测试
cd futures-order && mvn spring-boot:run
```

## 访问地址

| 组件 | 地址 | 账号 |
|------|------|------|
| Nacos | http://localhost:8848/nacos | nacos/nacos |
| Nacos (gRPC) | localhost:9848 | - |
| RocketMQ Dashboard | http://localhost:8088 | - |
| SkyWalking UI | http://localhost:8089 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/futures123 |
| MySQL | localhost:3306 | futures/futures123 |
| MySQL Slave | localhost:3307 | futures/futures123 |
| Redis | localhost:6379 | futures123 |
| Kafka | localhost:9092 | - |
| Seata | localhost:8091 | - |
| Elasticsearch | localhost:9200 | - |

## 基础设施启动建议

### 开发环境（当前）
- 使用 `docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker`
- 微服务直接运行在本地 IDE 或 screen 中
- 见 `docker-compose.yml` 顶部「开发用最小集合」

### 测试/CI 环境
- 完整的 `docker compose up -d`（全部中间件）
- 微服务容器化运行

### 生产环境
- 参考 `infrastructure/k8s/` 配置
- 使用 Kubernetes 集群部署
- Nacos 3 节点 + MySQL 主从 + Redis 哨兵

## 技术栈图

```
┌──────────────────────────────────────────────────────────────┐
│                期货交易平台 技术栈全景图                        │
├──────────────┬───────────────────────────────────────────────┤
│  前端         │  React 18 + TypeScript + Vite + Ant Design   │
│              │  Lightweight Charts + WebSocket               │
├──────────────┼───────────────────────────────────────────────┤
│  API网关      │  Spring Cloud Gateway 2023.0.x               │
├──────────────┼───────────────────────────────────────────────┤
│  服务发现/配置 │  Nacos 2.3.x (gRPC协议)                     │
├──────────────┼───────────────────────────────────────────────┤
│  业务服务      │  Spring Boot 3.2.x + MyBatis-Plus 3.5.x     │
│              │  OpenFeign + Sentinel + Seata                 │
├──────────────┼───────────────────────────────────────────────┤
│  消息队列      │  RocketMQ 5.x (核心) + Kafka 3.x (备选)     │
├──────────────┼───────────────────────────────────────────────┤
│  数据层        │  MySQL 8.0 (主从) + Redis 7 (哨兵)          │
├──────────────┼───────────────────────────────────────────────┤
│  监控/可观测   │  SkyWalking + Prometheus + Grafana           │
└──────────────┴───────────────────────────────────────────────┘
```
