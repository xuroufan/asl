# 期货交易平台 — 部署方案

## 1. 蓝绿部署策略

### 架构

```
                ┌──────────┐
                │  Load    │
                │ Balancer │
                │ (Gateway)│
                └────┬─────┘
                     │
          ┌──────────┼──────────┐
          │          │          │
    ┌─────▼─────┐  ┌▼─────────┐
    │  Blue Env  │  │  Green Env│
    │ (v1.0.0)   │  │ (v1.1.0)  │
    │ 8088 -> V1 │  │ 9099 -> V2│
    └───────────┘  └──────────┘
```

### 流程

```bash
# 1. 部署新版本到 Green 环境
docker compose -f docker-compose.green.yml up -d

# 2. 验证 Green 环境
curl http://localhost:9099/actuator/health
curl http://localhost:9099/api/v1/actuator/health

# 3. 切换流量到 Green（修改Gateway路由）
# 在Nacos中修改 gateway 配置
# routes[0].uri → http://green-gateway:9099

# 4. 观察 15 分钟
# 确认错误率、延迟正常

# 5. 下线 Blue 环境
docker compose -f docker-compose.blue.yml down

# 6. 下次部署时互换角色
```

### 回滚

```bash
# 切换回 Blue
# 修改Nacos路由配置 → Blue环境
# 验证Green无问题后清理
docker compose -f docker-compose.green.yml down
```

## 2. 灰度发布（Canary Release）

### 架构

```
               ┌──────────┐
               │  Gateway │
               └────┬─────┘
                    │
          ┌─────────┼──────────┐
          │    95%  │    5%    │
    ┌─────▼─────┐  ┌▼─────────┐
    │  Stable   │  │  Canary  │
    │ (v1.0.0)  │  │ (v1.1.0) │
    │  4 replicas│  │  1 replica│
    └───────────┘  └──────────┘
```

### 实现方式

#### 方式A：Gateway权重路由（推荐）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: matching-route
          uri: lb://futures-matching
          predicates:
            - Path=/api/v1/matching/**
          filters:
            # 5%流量到canary
            - name: Weight
              args:
                group: matching
                weight: 95
```
配合Nacos元数据标记canary实例。

#### 方式B：单独Canary Gateway

```bash
# 启动Canary Gateway
docker compose up -d canary-gateway
# 启动Canary服务
docker compose up -d canary-matching canary-order ...
# Canary Gateway端口: 9088
# 外部分流通过SLB权重实现
```

### 验证指标

| 指标 | 通过标准 |
|------|----------|
| 错误率 | ≤ 当前生产环境 |
| P99延迟 | ≤ 当前生产环境 +10% |
| 业务成功率 | = 100% |
| 无业务报错 | 零用户投诉 |

## 3. K8s部署

### 前提条件

```bash
# 1. Docker化所有服务
docker build -t futures-platform/futures-gateway:latest -f Dockerfile.gateway .

# 2. 推送到镜像仓库
docker tag futures-platform/futures-gateway:latest registry.example.com/futures/gateway:v1.0.0
docker push registry.example.com/futures/gateway:v1.0.0

# 3. 创建命名空间
kubectl apply -f infrastructure/k8s/namespace.yaml

# 4. 修改模板中的镜像版本
sed -i '' 's/:latest/:v1.0.0/' infrastructure/k8s/*-deployment.yaml

# 5. 部署
kubectl apply -f infrastructure/k8s/
```

### K8s集群要求

| 组件 | 规格 |
|------|------|
| Master | 至少3节点 (4C/8G) |
| Worker | 至少5节点 (8C/16G) |
| 存储 | NFS/Ceph 持久卷 |
| 网络 | Calico 或 Flannel |
| Ingress | Nginx Ingress Controller |

## 4. CI/CD流水线

```mermaid
graph LR
    A[Git Push] --> B[GitHub Actions]
    B --> C[单元测试]
    C --> D[构建JAR]
    D --> E[Docker构建]
    E --> F[推送到Registry]
    F --> G[蓝绿部署]
    G --> H[冒烟测试]
    H --> I[切流]
    I --> J[监控观察]
    J --> K[完成/回滚]
```

### Jenkinsfile 或 GitHub Actions

```yaml
name: Deploy
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Test
        run: mvn clean package
      - name: Docker Build
        run: docker build -t futures-platform/${{ matrix.service }}:${{ github.sha }}
      - name: Deploy
        run: |
          kubectl set image deployment/${{ matrix.service }} \
            ${{ matrix.service }}=futures-platform/${{ matrix.service }}:${{ github.sha }}
```

## 5. 环境规划

| 环境 | 用途 | 配置 |
|------|------|------|
| dev | 开发自测 | 单机 Docker |
| test | 集成测试 | 3节点 Docker Swarm |
| staging | 预发布 | K8s 最小集群 |
| prod | 生产 | K8s 高可用集群 |

## 6. 数据库迁移策略

```bash
# 迁移步骤
# 1. 新版本 init-schema.sql 需要向前兼容
# 2. 只允许 ADD COLUMN, 不允许 DROP/RENAME
# 3. 大表DDL使用 pt-online-schema-change

# 示例: 在线加列
pt-online-schema-change \
  h=futures-mysql-master,u=futures,p=futures123,D=futures_fund,t=t_fund_account \
  --alter "ADD COLUMN new_column VARCHAR(50)" \
  --execute
```
