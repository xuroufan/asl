# 部署指南

## 前置条件

| 组件 | 版本 | 检查 |
|------|------|------|
| JDK | 21+ | `java -version` |
| Docker Desktop | latest | `docker ps` |
| Node.js | 18+ | `node -v` |
| tmux | 3.x | `tmux -V` |
| Maven | 3.9+ | `mvn -v` |

## 本地开发 (macOS)

### 1. 启动基础设施

```bash
# 启动 Docker 桌面
open -a Docker

# 启动 MySQL + Redis
cd backend
docker compose up -d mysql-master redis-master

# 验证
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### 2. 初始化数据库

```bash
docker exec -i futures-mysql-master mysql -u root -pfutures123 \
  < backend/infrastructure/scripts/init-schema.sql

# 插入管理后台种子数据
docker exec -i futures-mysql-master mysql -u root -pfutures123 futures_admin \
  < backend/futures-admin/src/main/resources/schema.sql

docker exec -i futures-mysql-master mysql -u root -pfutures123 futures_admin -e "
INSERT INTO sys_user (user_id, username, password, nickname, status) VALUES
(1, 'admin', '\$2a\$10\$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 0)
ON DUPLICATE KEY UPDATE username=username;
"
```

### 3. 编译

```bash
cd backend
mvn package -Dmaven.test.skip=true -T 4
```

> 首次编译需下载依赖，耗时 3-5 分钟

### 4. 启动全部服务

```bash
# 一键启动（推荐）
bash start.sh start

# 或者手动分步启动
bash start.sh start     # 启动所有微服务
bash start.sh status    # 验证健康状态
```

### 5. 启动前端

```bash
# Web 行情终端
cd web && tmux new-session -d -s web "npm run dev -- --port 5173 --host"

# 管理后台
cd admin-ui && tmux new-session -d -s admin-ui "npm run dev -- --port 8090 --host"
```

## 管理命令

```bash
bash start.sh              # 显示帮助
bash start.sh start        # 启动全部
bash start.sh stop         # 停止全部
bash start.sh restart      # 重启
bash start.sh status       # 查看状态
bash start.sh logs <svc>   # 查看日志 (order/matching/account/...)
```

## Docker 镜像构建

```bash
# 构建并推送到 GHCR
bash tools/docker-build.sh
```

构建 10 个微服务镜像，推送到 `ghcr.io/xuroufan/asl/<service>:latest`

## 生产部署要点

### 网络
- API 网关需暴露 8088 端口
- WebSocket 走 `/ws/**` 路由
- Prometheus 指标走 `/actuator/prometheus`

### 安全
- JWT 密钥通过环境变量 `JWT_SECRET` 注入
- 数据库密码通过 `DB_PASSWORD` 环境变量
- 生产应使用 Seata/Nacos（开发环境已禁用）

### 监控
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/futures123)
- 内置健康端点: `/actuator/health`

### 数据库
- 8 个分库: `futures_order`, `futures_account`, `futures_fund`, `futures_risk`, `futures_market`, `futures_settlement`, `futures_admin`, `futures_push`
- 备份: `bash tools/backup.sh`（每日 3:00 自动备份，保留 7 天）

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_SECRET` | `futures-admin-secret-key-...` | JWT 签名密钥 |
| `DB_PASSWORD` | `futures123` | MySQL 密码 |
| `REDIS_PASSWORD` | `futures123` | Redis 密码 |
| `MYSQL_ROOT_PASSWORD` | `futures123` | MySQL root 密码 |
| `GRAFANA_PASSWORD` | `futures123` | Grafana 密码 |
