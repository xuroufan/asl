# 期货交易平台 — 运维手册

## 1. 系统架构总览

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  tws-lite-web │     │futures-term.│     │  admin-ui    │
│  (3001 React)  │     │(5173 React)  │     │(5174 Vue3)   │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       └────────────────┬───┴────────────────────┘
                        │
                 ┌──────▼───────┐
                 │  API Gateway  │  8088
                 │  (2 replicas) │
                 └──────┬───────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
   ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
   │ Account  │    │  Order   │    │  Market  │
   │ :8083    │    │  :8081   │    │  :8086   │
   └─────────┘    └─────────┘    └─────────┘
   ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
   │  Fund   │    │ Matching│    │  Risk   │
   │ :8084   │    │  :8082   │    │  :8085   │
   └─────────┘    └─────────┘    └─────────┘
                ┌────▼────┐
                │Settlement│
                │  :8087   │
                └─────────┘
```

## 2. 日常操作

### 2.1 启动服务

```bash
# 启动基础设施（MySQL, Redis, Nacos, RocketMQ）
cd futures-platform
docker compose up -d mysql-master redis-master nacos rocketmq-ns

# 启动微服务（按依赖顺序）
cd futures-platform
./infrastructure/scripts/service.sh start

# 启动前端
cd tws-lite-web   && npx vite --port 3001 &
cd futures-terminal && npx vite --port 5173 &
cd futures-admin-ui && npx vite --port 5174 &
```

### 2.2 停止服务

```bash
./infrastructure/scripts/service.sh stop
docker compose down  # 停止所有Docker容器
```

### 2.3 查看状态

```bash
./infrastructure/scripts/service.sh status
curl localhost:8088/actuator/health    # 网关
curl localhost:8848/nacos/v1/ns/service/list  # Nacos服务列表
```

### 2.4 查看日志

```bash
# 实时日志
tail -f /tmp/fm-*.log

# 按服务
tail -f /tmp/fm-gateway.log
tail -f /tmp/fm-matching.log

# 错误日志
grep -i "ERROR\|Exception" /tmp/fm-*.log
```

## 3. 监控系统

### 3.1 访问地址

| 系统 | 地址 | 凭证 |
|------|------|------|
| Grafana | http://localhost:3002 | admin / futures123 |
| Prometheus | http://localhost:9090 | — |
| Loki | http://localhost:3100 | — |
| Kibana (ELK) | http://localhost:5601 | — |
| AlertManager | http://localhost:9093 | — |

### 3.2 Grafana 仪表盘

启动后需手动导入以下仪表盘：
1. **JVM Dashboard** — Micrometer JVM监控
2. **Spring Boot Dashboard** — Spring Boot状态
3. **Loki Logs Dashboard** — 日志查询
4. **Business Dashboard** — 交易业务指标

```bash
# 或通过API自动导入（需替换API_KEY）
curl -X POST http://localhost:3002/api/dashboards/db \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d @dashboard.json
```

## 4. 告警处理

### 4.1 告警通道

告警通过 Prometheus → AlertManager 路由：
- **Slack #futures-critical** — P1关键告警
- **Slack #futures-alerts** — P2/P3告警
- **邮件** — 值班人员通知

### 4.2 告警确认

```bash
# 通过 AlertManager API 静默告警
curl -X POST http://localhost:9093/api/v1/silences \
  -H "Content-Type: application/json" \
  -d '{"matchers":[{"name":"alertname","value":"ServiceDown"}],"startsAt":"2026-01-01T00:00:00Z","endsAt":"2026-01-02T00:00:00Z","createdBy":"admin","comment":"维护窗口"}'
```

### 4.3 常见告警处理

| 告警 | 处理步骤 |
|------|----------|
| ServiceDown | 1.检查进程 `ps aux | grep java`  2.查看日志 `tail -100 /tmp/fm-xxx.log`  3.重启服务 |
| HighHeapMemoryUsage | 1.登录Grafana查看JVM面板  2.检查GC暂停时间  3.确认是否有内存泄漏  4.增加Xmx或重启 |
| HighErrorRate | 1.查看API错误日志  2.检查数据库连接池  3.检查下游服务依赖 |
| GatewayRouteDown | 1.检查Nacos服务列表  2.检查目标服务健康  3.重启目标服务 |
| DiskUsage | 1.清理日志 `find /tmp -name "*.log" -mtime +7 -delete`  2.检查备份空间  3.扩展磁盘 |

## 5. 备份与恢复

### 5.1 手动备份

```bash
# MySQL
./infrastructure/scripts/backup-mysql.sh

# Redis
./infrastructure/scripts/backup-redis.sh
```

### 5.2 安装定时备份

```bash
sudo ./infrastructure/scripts/install-backup-cron.sh
```

### 5.3 恢复

```bash
# MySQL恢复
gunzip < /data/backup/mysql/20260101_030000/futures_all_databases.sql.gz | mysql -uroot -pfutures123

# Redis恢复
gunzip -k /data/backup/redis/20260101_040000/dump.rdb.gz
cp /data/backup/redis/20260101_040000/dump.rdb /var/lib/redis/dump.rdb
redis-cli DEBUG RELOAD  # 或重启Redis
```

## 6. 数据库管理

### 6.1 连接信息

```
MySQL Master:  localhost:3306  futures/futures123
MySQL Slave:   localhost:3307  futures/futures123
Redis Master:  localhost:6379  futures123
```

### 6.2 Schema迁移

每次代码更新后，比对实体与数据库：

```bash
# 检查差异
docker exec futures-mysql-master mysql -u futures -pfutures123 \
  -e "DESCRIBE futures_fund.t_fund_account;"

# 手动执行ALTER TABLE
# 详见 infrastructure/scripts/init-schema.sql
```

## 7. 容量规划

### 当前配置

| 组件 | 规格 | 备注 |
|------|------|------|
| 微服务 | 256MB-512MB | 每个JVM |
| MySQL | 1GB | Docker |
| Redis | 512MB | Docker |
| Nacos | 512MB | Standalone |
| RocketMQ | 1GB Broker | Docker |
| Prometheus | 512MB | 保留15天 |

### 扩容建议

- 订单量 > 10000笔/日 → JVM增至1GB
- 并发用户 > 1000 → Gateway增加replica
- 数据量 > 100GB → MySQL分表
- 指标数据 > 50GB → Prometheus远端存储 (Thanos)
