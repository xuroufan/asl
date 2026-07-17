# 灾备恢复计划 (Disaster Recovery Plan)

> 版本: v1.0 | 更新: 2026-07-13

---

## 1. 故障等级定义

| 等级 | 定义 | 响应时间 | 恢复目标 |
|------|------|---------|---------|
| P0 | 核心交易不可用 | ≤ 5 分钟 | RTO < 15min, RPO < 1min |
| P1 | 部分功能不可用（如 K 线图表）| ≤ 15 分钟 | RTO < 1h, RPO < 5min |
| P2 | 非关键功能不可用（如历史查询）| ≤ 1 小时 | RTO < 4h, RPO < 1h |
| P3 | 体验问题（如 UI 异常）| ≤ 1 工作日 | 下个版本修复 |

## 2. 故障场景与恢复步骤

### 2.1 数据库故障（MySQL）

**症状**: 下单超时、查询失败、Nacos 无法启动
**恢复步骤**:
```bash
# 1. 检查主库状态
docker exec futures-mysql-master mysqladmin status

# 2. 如果主库宕机，提升从库为主库
docker exec futures-mysql-slave mysql -e "STOP SLAVE; RESET SLAVE ALL;"
# 更新应用配置指向从库 IP

# 3. 从备份恢复
docker exec -i futures-mysql-master mysql -uroot -p<password> futures_order < /backup/mysql/futures_order_20260713.sql

# 4. 重新建立主从关系
docker exec futures-mysql-master mysql -e "GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';"
docker exec futures-mysql-slave mysql -e "CHANGE MASTER TO MASTER_HOST='futures-mysql-master'; START SLAVE;"
```

### 2.2 Redis 故障

**症状**: 限流失效、会话丢失、行情缓存消失
**恢复步骤**:
```bash
# 1. Sentinel 应该自动完成故障转移
docker exec futures-redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster

# 2. 如果 Sentinel 也挂了，手动启动从库
docker exec futures-redis-slave-1 redis-cli SLAVEOF futures-redis-master 6379

# 3. 从 RDB 备份恢复
docker exec -i futures-redis-master redis-cli < /backup/redis/dump-20260713.rdb
```

### 2.3 Nacos 故障

**症状**: 服务发现失败、配置中心不可用
**恢复步骤**:
```bash
# 1. 单节点重启
cd /Users/fangfang/Documents/黑期/nacos-server && bash bin/startup.sh -m standalone

# 2. 从 MySQL 备份恢复 Nacos 配置库
mysql -ufutures -p nacos_config < /backup/nacos/nacos_config_20260713.sql

# 3. 配置缓存（所有微服务本地缓存 bootstrap.yml）
# 即使 Nacos 不可用，微服务仍能使用上次缓存的配置运行
```

### 2.4 消息队列故障 (RocketMQ)

**症状**: 订单状态不同步、成交回报延迟
**恢复步骤**:
```bash
# 1. 重启 Broker
docker restart futures-rocketmq-broker

# 2. 检查消费进度
docker exec futures-rocketmq-ns mqadmin consumerProgress -n localhost:9876 -g ORDER_CONSUMER_GROUP

# 3. 重置消费位点（允许重新消费）
docker exec futures-rocketmq-ns mqadmin resetOffsetByTime -n localhost:9876 -g ORDER_CONSUMER_GROUP -t ORDER_TOPIC -timestamp 0
```

## 3. 备份策略

### 3.1 MySQL 备份
```bash
# 全量备份（每天 03:00）
0 3 * * * /opt/futures/infrastructure/scripts/backup-mysql.sh

# 备份保留 30 天
# RPO: 最近一次全量备份时间（最大 24h）
```

### 3.2 Redis 备份
```bash
# RDB 快照备份（每小时）
0 * * * * /opt/futures/infrastructure/scripts/backup-redis.sh

# RPO: 最近一次 RDB 保存时间（最大 1h）
```

### 3.3 Nacos 配置备份
```bash
# 配置导出（每天 04:00）
0 4 * * * curl -X GET 'http://localhost:8848/nacos/v1/console/configs?export=true&tenant=public' -o /backup/nacos/nacos-configs-$(date +%Y%m%d).zip

# RPO: 最大 24h
```

## 4. 跨可用区部署架构

```
┌──────────────────────┐     ┌──────────────────────┐
│   可用区 A (主)       │     │   可用区 B (备)       │
│                      │     │                      │
│  Nacos Cluster       │     │  Nacos Cluster       │
│  MySQL Master        │◄───►│  MySQL Slave          │
│  Redis Master        │     │  Redis Slave          │
│  Gateway + 微服务    │     │  Gateway + 微服务    │
│  RocketMQ Master     │     │  RocketMQ Slave       │
└──────────────────────┘     └──────────────────────┘
           │                          │
           └─────────────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   SLB / DNS     │
                    │  故障自动切换    │
                    └─────────────────┘
```

## 5. 演练计划

| 场景 | 频率 | 参与方 | 目标 |
|------|------|--------|------|
| MySQL 主从切换 | 每月 | DBA + 开发 | 5 分钟内完成切换 |
| 单节点 Nacos 宕机 | 每季 | 运维 | 服务不受影响 |
| 完整灾备演练 | 每半年 | 全体 | RTO/RPO 达标 |
| Redis 缓存雪崩 | 每季 | 开发 | 降级策略验证 |

## 6. 联系人

| 角色 | 姓名 | 电话 | 备用 |
|------|------|------|------|
| 值班运维 | — | — | — |
| DBA | — | — | — |
| 架构师 | — | — | — |
| 产品经理 | — | — | — |
