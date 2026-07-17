# 期货交易平台 — 人工操作清单

> 版本: v1.0 | 最后更新: 2026-07-13  
> 用途: 所有需要人工干预的部署、配置、优化任务清单

---

## 目录

1. [基础设施搭建](#1-基础设施搭建)
2. [凭据与安全](#2-凭据与安全)
3. [监控与告警](#3-监控与告警)
4. [备份与灾备](#4-备份与灾备)
5. [CI/CD 流水线](#5-cicd-流水线)
6. [后端服务](#6-后端服务)
7. [前端交付](#7-前端交付)
8. [性能测试](#8-性能测试)
9. [文档与交接](#9-文档与交接)

---

## 1. 基础设施搭建

### 1.1 云账号与服务

- [ ] 申请云服务商账号（阿里云 / AWS / 腾讯云）
- [ ] 创建至少 2 个可用区（用于跨 AZ 部署）
- [ ] 申请域名并配置 DNS
- [ ] 申请 SSL/TLS 证书（阿里云免费证书 / Let's Encrypt / AWS ACM）
- [ ] 创建对象存储 Bucket（OSS / S3，用于备份存储）

### 1.2 依赖服务

- [ ] **MySQL**: 部署主从架构（至少 2 节点）
- [ ] **Redis**: 部署 Sentinel 集群（至少 3 节点）
- [ ] **Nacos**: 部署集群模式（至少 3 节点）→ 参考 `infrastructure/nacos-cluster.md`
- [ ] **RocketMQ**: 部署 Broker 集群 + NameServer
- [ ] **Kafka**: 部署 3 节点集群（用于事件溯源）
- [ ] **SkyWalking**: 部署 OAP Server + Elasticsearch 存储 → `infrastructure/skywalking/`
- [ ] **ProxySQL**: 配置数据库读写分离 → `infrastructure/scripts/proxysql.cnf`
- [ ] **Seata**: 部署 TC Server → 参考 `seata-server/`

### 1.3 Docker 基础镜像

- [ ] 构建所有微服务的 Docker 镜像（Dockerfile 已就绪）
- [ ] 推送镜像到私有仓库（阿里云 ACR / Docker Hub / Harbor）
- [ ] 标记镜像版本号（遵守语义化版本）

---

## 2. 凭据与安全

### 2.1 密钥管理

| 凭据 | 当前状态 | 操作 |
|------|---------|------|
| MySQL 密码 `futures123` | 占位符 | 修改为强密码并记录 |
| Redis 密码 `futures123` | 占位符 | 修改为强密码并记录 |
| Nacos 密码 `nacos/nacos` | 默认值 | 登录控制台修改 |
| JWT 签名密钥 | `${JWT_SECRET}` | 生成 RSA 256 位密钥对 |
| TLS 证书 | `certs/*.pem` 占位 | 替换为真实证书 |
| SMTP 密码 | `${SMTP_PASSWORD}` | 配置真实 SMTP 密码 |

- [ ] 申请 Secret 管理平台（Vault / AWS Secrets Manager / 阿里云 KMS）
- [ ] 将所有密钥迁移到 Secret 管理平台
- [ ] 配置密钥自动轮换策略（建议 90 天）
- [ ] 阅读 `infrastructure/secrets-migration.md` 参考三种迁移方案

### 2.2 网络安全

- [ ] 配置 Nginx HTTPS → 参考 `docker/nginx-ssl.conf`
- [ ] 将认证服务端口 `8083` 改为仅内网监听
- [ ] 配置 API 网关 IP 白名单（管理后台限制特定 IP）
- [ ] 配置数据库仅允许内网连接
- [ ] 配置 Redis 密码认证 + 绑定内网 IP
- [ ] 配置 Nacos 认证（`nacos.core.auth.enabled=true`）
- [ ] 配置防火墙规则（仅暴露 443/80 端口到公网）

### 2.3 应用安全

- [ ] 删除所有 `application.yml` / `.env` 中的明文密码
- [ ] 所有密码改为 `${VAR_NAME}` 环境变量引用
- [ ] CI/CD 中使用 GitHub Secrets / GitLab CI Variables
- [ ] Docker Compose 中使用 `secrets:` 而非环境变量

---

## 3. 监控与告警

### 3.1 Prometheus + Grafana

- [ ] 确认 Prometheus 已启动（端口 9090）
- [ ] 确认 Grafana 已启动（端口 3002，默认账号 admin/admin）
- [ ] **修改 Grafana 默认密码**
- [ ] 导入 Grafana 仪表盘 → `infrastructure/grafana/dashboards/futures-platform-overview.json`
- [ ] 配置 Prometheus 抓取目标（确认所有服务 endpoint 可达）
- [ ] 验证 JVM 指标（jvm_memory_used_bytes, jvm_gc_*）是否正常上报

### 3.2 告警通道

| 通道 | 当前状态 | 操作 |
|------|---------|------|
| Email (SMTP) | `smtp.example.com` 占位 | 配置真实 SMTP 服务器 |
| Slack | 模板已创建 | 创建 Incoming Webhook |
| 钉钉 | 模板已创建 | 创建自定义机器人 + 部署 dingtalk-hook |
| PagerDuty | 模板已创建 | 创建 PagerDuty 集成 |

- [ ] 填写 AlertManager 配置中 `smtp_smarthost` 为真实 SMTP 地址
- [ ] 创建 Slack Webhook → 填入 `alertmanager.yml`
- [ ] 创建钉钉机器人 → 部署 `timonwong/prometheus-webhook-dingtalk`
- [ ] 部署并验证 AlertManager 告警推送
- [ ] 阅读 `infrastructure/scripts/alertmanager-channels.yml` 获取模板

### 3.3 日志聚合

- [ ] 确认 ELK / Loki 已启动
- [ ] 配置 Logstash 日志采集 pipeline → `infrastructure/logstash/pipeline/futures.conf`
- [ ] 配置 Loki promtail → `infrastructure/loki/promtail-config.yaml`
- [ ] 配置日志保留策略（建议 30 天）
- [ ] 在 Grafana 中配置 Loki 数据源

### 3.4 链路追踪

- [ ] 部署 SkyWalking OAP Server + Elasticsearch
- [ ] 配置微服务 SkyWalking Agent（agent.config 已就绪）
- [ ] 验证链路数据上报
- [ ] 在 SkyWalking UI 中查看拓扑图

---

## 4. 备份与灾备

### 4.1 备份配置

- [ ] 设置 MySQL 定时备份（`backup-mysql.sh`）
- [ ] 设置 Redis 定时备份（`backup-redis.sh`）
- [ ] 设置 Nacos 配置定时导出
- [ ] 配置备份文件上传到**异地**对象存储（OSS / S3）
- [ ] 验证备份恢复流程（至少每月一次）

### 4.2 灾备配置

- [ ] 阅读 `disaster-recovery.md`
- [ ] 部署跨可用区架构（至少 2 台云服务器）
- [ ] 配置 MySQL 主从跨 AZ 复制
- [ ] 配置 Redis Sentinel 跨 AZ
- [ ] 配置 SLB / DNS 故障自动切换
- [ ] 制定演练计划（每月切换演练）

---

## 5. CI/CD 流水线

### 5.1 GitHub Actions

- [ ] 配置 GitHub Secrets（Docker 仓库密钥、SSH 密钥、环境变量）
- [ ] 启用 `.github/workflows/ci.yml`（如有）
- [ ] 启用 `.github/workflows/deploy.yml`（如有）
- [ ] 配置 Docker 镜像自动构建
- [ ] 配置代码质量检查（lint / type-check / test）

### 5.2 部署流水线

- [ ] 配置 CI 流水线步骤：`build → test → package → push image`
- [ ] 配置 CD 流水线步骤：`pull image → blue-green deploy → health check → switch traffic`
- [ ] 配置回滚策略（保留 3 个历史版本）
- [ ] 验证 `deploy-bluegreen.sh` 脚本
- [ ] 配置 Nginx upstream 实现无损切换 → `docker/nginx.conf`

### 5.3 流水线检查点

- [ ] 构建时运行单元测试（`mvn test` / `npm test`）
- [ ] 构建时检查性能预算（`performance-budget.json`）
- [ ] 部署前运行冒烟测试
- [ ] 部署后运行集成测试
- [ ] 构建产物保留策略（保留最近 30 个构建）

---

## 6. 后端服务

### 6.1 消息队列初始化

- [ ] 创建 RocketMQ Topic（订单、成交、行情）
  ```bash
  bash infrastructure/message-queue/rocketmq/topic-init.sh
  ```
- [ ] 创建 Kafka Topic（事件溯源）→ `infrastructure/message-queue/kafka/topic-init.sh`
- [ ] 验证消息收发

### 6.2 分布式事务

- [ ] 确认 Seata Server 已启动
- [ ] 初始化 Seata 数据库表（`seata-server/script/server/db/mysql.sql`）
- [ ] 配置微服务 Seata 分组
- [ ] 验证 AT 模式回滚

### 6.3 数据库

- [ ] 确认 `init-schema.sql` 已执行（`futures_order` / `futures_account` 等库）
- [ ] 确认 `nacos_config` 数据库已初始化
- [ ] 验证表名一致性（`fund_account` vs `t_fund_account` 等）

### 6.4 配置中心

- [ ] 将 Nacos 生产配置 `futures-shared-prod.yaml` 导入 Nacos
- [ ] 将各微服务 `*-prod.yaml` 配置导入 Nacos
- [ ] 验证配置动态刷新
- [ ] **删除 Nacos 默认的 `nacos/nacos` 用户**或修改密码

---

## 7. 前端交付

### 7.1 生产构建

- [ ] 运行完整生产构建：`npm run build`
- [ ] 验证所有 chunk 大小（总 gzip < 500 kB）
- [ ] 验证 sourcemap 不泄露源码
- [ ] 检查被标记的大 chunk（> 500 kB）

### 7.2 性能验证

- [ ] **在真实浏览器中运行 Lighthouse**：打开 Chrome DevTools → Lighthouse → 生成报告
- [ ] 验证桌面端评分 ≥ 90
- [ ] 验证移动端评分 ≥ 70
- [ ] 验证 FCP < 1.8s, LCP < 2.5s
- [ ] 运行性能测试脚本：`bash smoke-test.sh` / `bash load-test.sh`

### 7.3 前端框架集成分

| 组件 | 状态 | 操作 |
|------|------|------|
| Watchlist（自选股） | 代码已就绪 | 集成到 Trading 页面布局中 |
| RefreshRate（刷新频率） | Hook 已就绪 | 集成到 StatusBar 或设置面板 |
| Sounds（操作音效） | 工具已就绪 | 集成到 OrderPanel 下单回调中 |
| Drag Layout（拖拽布局） | 框架待创建 | 需安装 react-grid-layout 并实现 |

- [ ] 将 `Watchlist` 组件添加到 Trading 页面
- [ ] 将 RefreshRate 集成到状态栏
- [ ] 在 `handleOrderSuccess` 中调用 `playSound('success')`
- [ ] 创建拖拽布局面板

### 7.4 包体积优化

- [ ] 使用 `rollup-plugin-visualizer` 分析 bundle 构成
- [ ] 对 Ant Design Icons 使用按需导入（可减少 ui-vendor 约 60%）
- [ ] 使用 `lazy()` 进一步拆分非首屏重型组件（如 OrderHistoryModal）

---

## 8. 性能测试

### 8.1 压力测试

- [ ] 安装 k6 或 ab
  ```bash
  brew install k6  # macOS
  ```
- [ ] 执行撮合引擎压力测试
  ```bash
  k6 run load-test.js  # 目标: > 1000 TPS
  ```
- [ ] 执行 API 压力测试
  ```bash
  ab -n 10000 -c 100 http://localhost:8088/api/v1/market/symbols
  ```
- [ ] 验证 P99 响应时间 < 500ms
- [ ] 验证系统在 100+ 并发下不崩溃

### 8.2 稳定性测试

- [ ] 连续运行 2 小时监控内存增长（目标 < 50 MB）
- [ ] 验证 WebSocket 断线重连 < 3s
- [ ] 验证 Reids / MySQL 连接池不泄漏
- [ ] 验证消息队列不堆积

---

## 9. 文档与交接

### 9.1 必须交付文档

- [ ] **架构文档**: `infrastructure/architecture.md` ✅ 已存在
- [ ] **部署方案**: `doc/DEPLOYMENT.md` ✅ 已存在
- [ ] **运维手册**: `doc/OPS.md` ✅ 已存在
- [ ] **SLA/SLO**: `doc/SLA.md` ✅ 已存在
- [ ] **性能计划**: `doc/PERFORMANCE.md` ✅ 已存在
- [ ] **灾备计划**: `infrastructure/disaster-recovery.md` ✅ 已创建
- [ ] **凭据迁移**: `infrastructure/secrets-migration.md` ✅ 已创建
- [ ] **版本矩阵**: `infrastructure/version-matrix.md` ✅ 已存在
- [ ] **资源规格**: `infrastructure/resource-specs.md` ✅ 已存在
- [ ] **交付清单**: `DELIVERY_CHECKLIST.md` ✅ 已创建

### 9.2 待补充文档

- [ ] **值班手册**: 故障响应流程、联系人、升级路径
- [ ] **回滚手册**: 版本回退步骤
- [ ] **扩容手册**: 水平/垂直扩容步骤
- [ ] **API 文档**: 完整的接口文档（Swagger / OpenAPI）

### 9.3 知识转移

- [ ] 备份所有 `application.yml` 和 `bootstrap.yml` 到安全位置
- [ ] 记录所有管理员账号和初始密码
- [ ] 记录所有第三方服务的 API Key 和 Secret
- [ ] 组建值班群（钉钉/微信）并导入告警通知

---

## 优先级速查

```
P0 — 上线前必须完成
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 1.2 部署 MySQL / Redis / Nacos 生产集群
□ 2.1 配置 Secret 管理平台
□ 2.2 配置 HTTPS + 防火墙
□ 2.3 删除所有明文密码
□ 3.1 配置 Grafana + 修改密码
□ 3.2 配置至少一个告警通道
□ 4.1 配置定时备份
□ 6.4 修改 Nacos 默认密码
□ 7.2 运行 Lighthouse 验证

P1 — 上线后第一周完成
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 3.3 配置日志聚合
□ 4.2 配置跨可用区部署
□ 5.1 配置 CI/CD 流水线
□ 6.1 初始化消息队列 Topic
□ 6.2 验证分布式事务
□ 7.3 集成 Watchlist / RefreshRate / Sounds
□ 8.1 执行压力测试

P2 — 上线后第一个月完成
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 3.4 部署 SkyWalking 链路追踪
□ 5.3 配置冒烟测试 + 性能预算检查
□ 7.4 包体积二次优化
□ 8.2 稳定性测试
□ 9.2 补充值班/回滚/扩容手册
□ 9.3 知识转移 + 值班群组建
```

---

> 提示：已用 ✅ 标记的任务表示代码/脚本/文档已就绪，只需要执行命令或简单配置即可。
> 方框 □ 表示需要人工操作（创建账号、填写配置、部署资源等）。
