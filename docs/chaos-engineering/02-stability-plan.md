# 稳定性预案手册

> 目的: 在故障发生时所有 SRE 和开发人员有标准操作流程可循

---

## 场景 1：MySQL 主库宕机

### 影响
- 所有写操作失败（下单、撤单、资金变动）
- 读操作受从库延迟影响

### 应急处置

```bash
# Step 1: 确认主库状态
kubectl exec -it mysql-0 -- mysql -e "SHOW SLAVE HOSTS;"
kubectl exec -it mysql-0 -- mysql -e "SHOW STATUS LIKE 'Uptime';"

# Step 2: 检查从库延迟和日志位置
kubectl exec -it mysql-1 -- mysql -e "SHOW SLAVE STATUS\G" | grep -E "Seconds_Behind_Master|Master_Log_File|Read_Master_Log_Pos"

# Step 3: 将从库提升为主库
# 在从库上执行
kubectl exec -it mysql-1 -- mysql -e "STOP SLAVE; RESET SLAVE ALL;"

# Step 4: 更新应用连接串
kubectl patch configmap trading-db-config -p '{"data":{"DB_URL":"jdbc:mysql://mysql-1:3306/trading"}}'

# Step 5: 滚动重启所有微服务
kubectl rollout restart deployment -n trading-backend

# Step 6: 验证读写正常
curl http://order-service:8080/actuator/health

# Step 7: 恢复原主库（数据补齐后重新搭建主从）
# 在新主库上备份并恢复到原主库
```

### 修复后验证
- [ ] 所有微服务健康检查通过
- [ ] 下单/撤单功能正常
- [ ] 主从延迟 < 1s
- [ ] 告警解除

---

## 场景 2：Redis Sentinel 集群脑裂

### 影响
- 行情缓存读取失败
- 订单号和流水号生成中断
- 会话缓存丢失

### 应急处置

```bash
# Step 1: 检查各 Redis 节点状态
redis-cli -h redis-0 -p 6379 INFO sentinel
redis-cli -h redis-1 -p 6379 INFO sentinel
redis-cli -h redis-2 -p 6379 INFO sentinel

# Step 2: 仲裁确定正确的主库
# sentinel 的 leader 通过投票决定
# 查看 sentinel 的配置，手动指定正确的 master
kubectl exec redis-0 -- redis-cli SENTINEL GET-MASTER-ADDR-BY-NAME trading-cache

# Step 3: 如果脑裂导致数据不一致
# 选择数据最新的节点作为主库
# 在其他节点上执行
redis-cli -h redis-1 SLAVEOF redis-0 6379

# Step 4: 降级策略
# 如果 Redis 无法恢复，业务侧降级
# - 行情数据直接从 MySQL 读取（增加 200ms 延迟）
# - 订单 ID 改用雪花算法本地生成
# - 会话基于 Token 验证，跳过缓存
```

---

## 场景 3：Seata TC 不可用

### 影响
- 跨服务 TCC 事务无法协调
- 新的分布式事务无法开启
- 正在执行的事务可能卡住

### 应急处置

```bash
# Step 1: 重启 Seata Server
kubectl rollout restart statefulset seata-server -n trading-backend

# Step 2: 降级为最终一致性
# 配置文件中关闭 Seata（或通过 Nacos 动态配置）
# seata.enabled=false
# 开启 MQ 重试补偿
```

**降级后的一致性保障**:
- 下单流程：order-service 先写本地订单状态 "PENDING"，发 MQ 消息
- fund-service 消费 MQ：如果扣款成功则更新订单为 "FILLED"，否则发补偿消息
- 补偿消息：30 分钟后未完成则告警人工介入

---

## 场景 4：撮合引擎 OOM

### 影响
- matching-service Pod 反复 OOM Kill
- 订单无法撮合，订单簿状态丢失

### 应急处置

```bash
# Step 1: 保存现场
kubectl logs --previous matching-service-xxx > /tmp/matching-pre-crash.log

# Step 2: 从快照恢复订单簿（假设每 5 分钟快照一次到 Redis）
redis-cli -h redis-0 GET matching:symbol:HSI2309:snapshot

# Step 3: 重建订单簿
# 应用重启后从 Redis 加载快照 + 从 MySQL 恢复增量
kubectl rollout restart deployment matching-service

# Step 4: 验证撮合功能
curl -X POST http://matching-service:8080/actuator/health

# Step 5: 扩容
kubectl scale deployment matching-service --replicas=4
```

### OOM 根因排查
```bash
# 查看前一次 OOM 的堆转储
kubectl cp matching-service-xxx:/var/log/dump/heap-xxx.hprof ./heap.hprof
# 用 MAT 分析
```

---

## 场景 5：消息队列积压

### 影响
- 行情数据更新延迟
- 成交记录落库延迟
- 用户订单状态更新延迟

### 应急处置

```bash
# Step 1: 查看队列积压量
rabbitmqctl list_queues name messages_ready messages_unacknowledged

# Step 2: 临时增加消费者
kubectl scale deployment order-consumer --replicas=10

# Step 3: 如果积压太严重（>100万条）
# 跳过非关键消息（如行情历史数据）
# 只保留关键消息（订单、成交、资金）

# Step 4: 排查消费慢的原因
# - 检查 DB 是否有慢查询
# - 检查下游服务是否正常
```

---

## 场景 6：网关流量突增（DDoS / 刷单）

### 影响
- 网关 CPU 飙升
- 正常请求被限流失效
- 下游服务被打垮

### 应急处置

```bash
# Step 1: 在网关层启用全局限流
kubectl annotate ingress trading-gateway nginx.ingress.kubernetes.io/limit-rps=2000

# Step 2: 扩容 Gateway
kubectl scale deployment gateway-service --replicas=10

# Step 3: 在 WAF / CDN 层面拦截异常 IP
# 或通过 Sentinel 控制台动态修改限流规则

# Step 4: 确认异常流量来源后，在 K8s NetworkPolicy 中封禁
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: block-abuse-ip
  namespace: trading-backend
spec:
  podSelector:
    matchLabels:
      app: gateway-service
  ingress:
    - from:
      - ipBlock:
          cidr: 0.0.0.0/0
          except:
            - 1.2.3.4/32   # 被封禁的 IP
EOF
```

---

## 应急响应流程

```
故障发现 (告警/用户反馈)
    │
    ├─ T+0min: 确认故障等级
    │   P0/Critical → 立即响应，通知值班 SRE
    │   P1/Warning  → 15 分钟内响应
    │
    ├─ T+5min: 快速止损
    │   ├─ 切流（主从切换 / 降级）
    │   ├─ 扩容
    │   └─ 封禁异常 IP
    │
    ├─ T+15min: 排查根因
    │   ├─ 查看日志 (ELK: traceId 查询)
    │   ├─ 查看指标 (Grafana)
    │   ├─ 查看链路 (SkyWalking)
    │   └─ 确认影响面
    │
    ├─ T+30min: 修复 / 恢复
    │   ├─ 回滚变更 / 热修复
    │   ├─ 重启服务
    │   └─ 验证恢复
    │
    └─ T+48h: 复盘
        ├─ 根因分析 (RCA) 文档
        ├─ 改进措施 (Action Items)
        └─ 更新应急预案
```
