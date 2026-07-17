# Nacos 集群配置与服务注册指南

## 1. Nacos 集群部署

### 1.1 生产环境 3 节点集群

```yaml
# docker-compose 3节点集群配置
nacos-1:
  image: nacos/nacos-server:v2.3.2
  container_name: futures-nacos-1
  environment:
    MODE: cluster
    NACOS_SERVERS: "nacos-1:8848 nacos-2:8848 nacos-3:8848"
    MYSQL_SERVICE_HOST: mysql-master
    MYSQL_SERVICE_PORT: 3306
    MYSQL_SERVICE_USER: futures
    MYSQL_SERVICE_PASSWORD: futures123
    MYSQL_SERVICE_DB_NAME: nacos_config
    NACOS_AUTH_ENABLE: "true"
    NACOS_AUTH_TOKEN: "FuturesPlatformSecretKey2024ForJWT"
  ports:
    - "8848:8848"
    - "9848:9848"

nacos-2:
  image: nacos/nacos-server:v2.3.2
  container_name: futures-nacos-2
  environment:
    MODE: cluster
    NACOS_SERVERS: "nacos-1:8848 nacos-2:8848 nacos-3:8848"
    MYSQL_SERVICE_HOST: mysql-master
    ...

nacos-3:
  image: nacos/nacos-server:v2.3.2
  container_name: futures-nacos-3
  ...
```

### 1.2 Nacos 域名映射方案

```
nacos-1 → 192.168.1.101:8848
nacos-2 → 192.168.1.102:8848
nacos-3 → 192.168.1.103:8848

客户端访问: http://nacos-cluster:8848 (SLB 统一入口)
```

## 2. 微服务接入 Nacos 配置模板

### 2.1 bootstrap.yml (所有服务统一模板)

```yaml
spring:
  application:
    name: futures-order    # 每个服务替换为对应名称
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: ${NACOS_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:futures-platform}
        group: DEFAULT_GROUP
        heart-beat-interval: 30000     # 心跳间隔 30s
        heart-beat-retry: 3            # 重试次数
        weight: 1                      # 实例权重 (灰度发布时可调整)
        cluster-name: ${NACOS_CLUSTER:default}
        ephemeral: true                # 临时实例 (注册中心自动摘除)
      config:
        enabled: true
        server-addr: ${NACOS_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:futures-platform}
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true          # 配置热更新
```

### 2.2 各服务 bootstrap.yml 完整配置

<details>
<summary>futures-gateway/bootstrap.yml</summary>

```yaml
spring:
  application:
    name: futures-gateway
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: ${NACOS_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:futures-platform}
    gateway:
      routes:
        - id: auth-route
          uri: lb://futures-account    # 改为 lb:// 服务名
          predicates:
            - Path=/api/v1/auth/**
        - id: order-route
          uri: lb://futures-order
          predicates:
            - Path=/api/v1/order/**
        - id: matching-route
          uri: lb://futures-matching
          predicates:
            - Path=/api/v1/matching/**
        - id: fund-route
          uri: lb://futures-fund
          predicates:
            - Path=/api/v1/fund/**
        - id: risk-route
          uri: lb://futures-risk
          predicates:
            - Path=/api/v1/risk/**
        - id: market-route
          uri: lb://futures-market
          predicates:
            - Path=/api/v1/market/**
        - id: settlement-route
          uri: lb://futures-settlement
          predicates:
            - Path=/api/v1/settlement/**
        - id: account-route
          uri: lb://futures-account
          predicates:
            - Path=/api/v1/account/**
```
</details>

### 2.3 各服务 bootstrap.yml 配置要点

| 服务名 | application name | 说明 |
|--------|-----------------|------|
| futures-gateway | futures-gateway | API网关, 路由使用 lb:// |
| futures-account | futures-account | 用户账户服务 |
| futures-order | futures-order | 订单服务 |
| futures-matching | futures-matching | 撮合引擎 |
| futures-fund | futures-fund | 资金管理 |
| futures-risk | futures-risk | 风控引擎 |
| futures-market | futures-market | 行情服务 |
| futures-settlement | futures-settlement | 清结算 |

## 3. Nacos 命名空间与配置隔离

### 3.1 命名空间规划

| 命名空间 | 说明 | 用途 |
|---------|------|------|
| futures-platform (ID: 自动生成) | 业务命名空间 | 所有期货业务服务共享 |
| public | 公共组件 | Nacos 内置, 用于全局配置 |

### 3.2 Nacos 配置项清单

在 Nacos 配置中心中创建以下配置：

| Data ID | Group | 说明 |
|---------|-------|------|
| futures-datasource.yaml | DEFAULT_GROUP | 数据库连接配置 |
| futures-redis.yaml | DEFAULT_GROUP | Redis 配置 |
| futures-mq.yaml | DEFAULT_GROUP | 消息队列配置 |
| futures-seata.yaml | DEFAULT_GROUP | Seata 分布式事务配置 |
| futures-gateway.yaml | DEFAULT_GROUP | 网关路由配置(可选远程) |

## 4. 验证步骤

```bash
# 1. 启动 Nacos (Docker)
docker compose up -d nacos

# 2. 访问 Nacos 控制台
open http://localhost:8848/nacos
# 账号: nacos  密码: nacos

# 3. 启动微服务 (已接入 Nacos)
cd futures-order
mvn spring-boot:run -Dspring.cloud.nacos.discovery.enabled=true

# 4. 验证服务注册
curl http://localhost:8848/nacos/v1/ns/catalog/instances?serviceName=futures-order

# 5. 验证服务间调用 (通过服务名)
curl http://localhost:8088/api/v1/account/profile
```
