# ASL — 期货交易平台

全栈期货交易系统。覆盖 Android 交易客户端、Java 微服务后端、运营管理后台和 Web 行情终端。基于事件驱动架构和微服务拆分。

## 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android App                             │
│  Auth → Market → Trading → Positions → Push Notifications      │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP/WS
                    ┌──────▼──────┐
                    │  API Gateway │   futures-gateway (:8088)
                    │  JWT Auth   │   Rate Limiting
                    └──┬───┬───┬──┘
          ┌────────────┘   │   └──────────────┐
          ▼                ▼                  ▼
   ┌──────────┐    ┌────────────┐    ┌──────────────┐
   │  Auth    │    │   Order    │    │   Matching   │
   │  Account │    │   Trade    │    │   Disruptor  │
   │  (:8083) │    │  (:8081)   │    │   (:8082)    │
   └──────────┘    └─────┬──────┘    └──────────────┘
                         │               │
                    ┌────▼──────┐  ┌─────▼──────┐
                    │   Fund    │  │   Market   │
                    │  (:8084)  │  │  (:8086)   │
                    └───────────┘  └────────────┘
   ┌──────────┐    ┌────────────┐    ┌──────────────┐
   │   Risk   │    │ Settlement │    │    Push     │
   │  (:8085) │    │  (:8087)   │    │   WS (:8093)│
   └──────────┘    └────────────┘    └──────────────┘

   ┌──────────┐
   │  Admin   │ ◄── Admin UI (Vue 3, :8090)
   │  (:8099) │
   └──────────┘

   Web Terminal (React, :5173) ───► Gateway (:8088)
```

## 快速启动

```bash
# 1. 启动基础设施
open -a Docker                          # 启动 Docker Desktop
cd backend && docker compose up -d mysql-master redis-master

# 2. 初始化数据库
docker exec -i futures-mysql-master mysql -u root -pfutures123 \
  < infrastructure/scripts/init-schema.sql

# 3. 编译并启动服务
mvn package -Dmaven.test.skip=true -T 4
bash start.sh start

# 4. 启动前端
cd ../web       && tmux new-session -d -s web "vite --port 5173 --host"
cd ../admin-ui  && tmux new-session -d -s admin-ui "vite --port 8090 --host"
```

或者一键启动：
```bash
bash start.sh start
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| futures-order | 8081 | 订单生命周期管理 |
| futures-matching | 8082 | 撮合引擎 (LMAX Disruptor) |
| futures-account | 8083 | 账户、认证、KYC |
| futures-fund | 8084 | 保证金、出入金 |
| futures-risk | 8085 | 风控检查、强平 |
| futures-market | 8086 | 行情、K线 |
| futures-settlement | 8087 | 日终结算 |
| futures-gateway | 8088 | API 网关、JWT 鉴权 |
| futures-push | 8093 | WebSocket 推送 |
| futures-admin | 8099 | 运营管理后台 |
| Admin UI | 8090 | Vue 3 管理界面 |
| Web | 5173 | React 行情终端 |
| Grafana | 3000 | 监控面板 |
| Prometheus | 9090 | 指标采集 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

## 技术栈

| 层 | 技术 |
|------|------|
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit |
| Web | React 19, TypeScript, Tailwind CSS, Zustand |
| Admin | Vue 3, Element Plus, ECharts, Pinia |
| 后端 | Java 21, Spring Boot 3.2, Spring Cloud Gateway |
| 数据库 | MySQL 8.0, Redis 7, MyBatis-Plus |
| 消息 | RocketMQ, LMAX Disruptor |
| 监控 | Prometheus, Grafana, ELK |
| 容器 | Docker, GHCR |

## 文档

| 文档 | 说明 |
|------|------|
| [架构说明](docs/ARCHITECTURE.md) | 系统架构、数据流、事件驱动 |
| [部署指南](docs/DEPLOYMENT.md) | 从零到生产的完整部署步骤 |
| [API 文档](docs/API.md) | 网关路由、认证、WebSocket 协议 |
| [优化记录](docs/optimization-checklist.md) | Nacos、JVM、数据库优化历史 |
| [运维工具](tools/) | 备份、压测、构建、健康监控 |
| [Knife4j UI](http://localhost:8099/doc.html) | 管理后台 API 交互文档 (112 端点) |
| [健康监控](health.html) | 实时服务状态页面 |
