# ASL — 期货交易平台

全栈期货交易系统，覆盖 Android 交易客户端、Java 微服务后端、运营管理后台和行情 Web 端。基于事件驱动架构和微服务拆分，支持账户管理、行情推送、订单撮合、风控检查、结算清收等核心交易链路。

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| Android 客户端 | Kotlin, Compose, Hilt, KSP | 交易 App（dev/staging/prod 三环境） |
| 管理后台前端 | Vue 3, Element Plus, Vite | 运营/风控/财务/CRM 管理界面 |
| 行情 Web 端 | React, TypeScript, Zustand | K 线图、盘口、交易面板 |
| 微服务后端 | Java 17, Spring Boot, Maven | 11 个微服务 + API 网关 |
| 基础设施 | Docker, K8s, Nacos, SkyWalking | 服务治理、可观测性、CI/CD |

---

## 后端微服务

| 服务 | 职责 |
|------|------|
| futures-gateway | API 网关、限流、鉴权 |
| futures-account | 账户管理、认证、KYC、权限 |
| futures-fund | 资金管理、出入金 |
| futures-market | 行情接入、K 线聚合 |
| futures-matching | 订单撮合引擎 |
| futures-order | 订单生命周期管理 |
| futures-risk | 风控检查、强平计算 |
| futures-push | WebSocket 实时推送 |
| futures-settlement | 日终结算、对账、报表 |
| futures-admin | 运营后台 API |

---

## Android 客户端

### 模块

- `:app` — 主工程，三环境（dev/staging/prod）
- `:core:network` — 网络层（Retrofit + OkHttp）
- `:core:database` — 本地持久化（Room）
- `:core:model` — 数据模型
- `:core:util` — 通用工具
- `:feature:auth` — 登录/注册
- `:feature:market` — 行情展示
- `:feature:trading` — 交易下单
- `:feature:position` — 持仓管理

### 构建

```bash
./gradlew assembleDevDebug         # Dev Debug APK
./gradlew testDevDebugUnitTest     # 单元测试
./gradlew lintDevDebug             # 代码检查
```

---

## 前端

```bash
cd admin-ui && npm install && npm run dev   # 管理后台
cd web && npm install && npm run dev        # 行情 Web 端
```

---

## 基础设施

```bash
docker compose -f docker/dev-infra.yml up -d
```

- **Nacos** — 服务注册发现 & 配置中心
- **RocketMQ** — 异步消息 & 事件驱动
- **Sentinel** — 流量控制 & 熔断降级
- **Seata** — 分布式事务
- **SkyWalking** — 链路追踪 & APM
- **Prometheus + Grafana** — 监控告警

---

## CI/CD

GitHub Actions 自动运行 CI（push/PR 到 main）：lint → 单元测试 → 构建 Debug APK。自动备份每 10 小时执行一次。

---

## 文档

详细文档见 [`docs/`](docs/) 目录，涵盖混沌工程、数据库优化、JVM 调优、微服务治理、可观测性。

---

## 项目结构

```
.
├── app/                          # Android 主工程
├── core/                         # Android 核心模块（network/database/model/util）
├── feature/                      # Android 功能模块（auth/market/trading/position）
├── backend/                      # Java 微服务后端（11 个服务）
│   ├── futures-account/
│   ├── futures-admin/
│   ├── futures-common/
│   ├── futures-fund/
│   ├── futures-gateway/
│   ├── futures-market/
│   ├── futures-matching/
│   ├── futures-order/
│   ├── futures-push/
│   ├── futures-risk/
│   ├── futures-settlement/
│   └── infrastructure/           # 基础设施配置
├── admin-ui/                     # 管理后台 (Vue 3 + Element Plus)
├── web/                          # 行情 Web 端 (React + TypeScript)
├── docker/                       # Docker 编排
├── docs/                         # 全量文档
├── events/                       # 事件日志
├── .github/workflows/            # CI/CD 流水线
├── build.gradle.kts              # Android 根构建脚本
└── settings.gradle.kts           # Android 模块配置
```

---

*内部项目*
