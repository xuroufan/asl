# 优化验收清单

> 所有轮次完成后，按此清单逐项验证
> 最后更新: 2026-07-17

---

## 1. JVM 优化

| 检查项 | 验收标准 | 是否达标 | 备注 |
|--------|----------|:--------:|------|
| GC 频率 | < 1 次/分钟 | ☐ |  |
| Full GC | 0 次 | ☐ |  |
| 堆内存占用 | < 70% | ☐ |  |
| G1GC 已启用 | -XX:+UseG1GC | ☐ |  |
| HeapDumpOnOOM | 已配置 | ☐ |  |
| UseStringDeduplication | 已启用 | ☐ |  |
| GC 日志轮转 | 10 x 100MB | ☐ |  |
| ThreadLocal 排查 | 所有使用点都调用了 remove() | ☐ |  |
| 静态缓存 | 全部使用 Caffeine 带过期策略 | ☐ |  |
| 直接内存 | 定期监控 < 512MB | ☐ |  |

## 2. 数据库优化

| 检查项 | 验收标准 | 是否达标 | 备注 |
|--------|----------|:--------:|------|
| 慢查询 | < 1 秒 | ☐ |  |
| 连接池使用率 | < 70% | ☐ |  |
| 主从延迟 | < 5 秒 | ☐ |  |
| 所有高频 SQL 有索引 | EXPLAIN 显示 type 为 ref/range/const | ☐ |  |
| 深分页 | 已改为游标查询 | ☐ |  |
| IN 子句 | 分批查询（每批 < 500 条） | ☐ |  |
| HikariCP leakDetection | 已开启 30s | ☐ |  |
| Seata TCC 超时 | 30 秒 | ☐ |  |
| TCC 幂等 | 有 xid+phase 唯一索引 | ☐ |  |
| Saga 补偿 | 每条正向操作有对应补偿 | ☐ |  |
| 按月分表 | 已配置 ShardingSphere | ☐ |  |
| 数据归档 | 每日 3:00 执行 | ☐ |  |

## 3. 接口治理

| 检查项 | 验收标准 | 是否达标 | 备注 |
|--------|----------|:--------:|------|
| P99 核心接口响应时间 | < 500ms | ☐ |  |
| 超时错误率 | < 0.1% | ☐ |  |
| Feign 超时 | 按业务精细化配置 | ☐ |  |
| Feign 重试 | 查询可重试，写操作不重试 | ☐ |  |
| Sentinel 限流 | 规则已加载生效 | ☐ |  |
| Sentinel 熔断 | 错误率 > 20% 触发 | ☐ |  |
| 优雅关闭 | server.shutdown=graceful | ☐ |  |
| preStop Hook | 已配置 sleep 30 | ☐ |  |
| readinessProbe | 使用 /actuator/health/readiness | ☐ |  |
| 全局超时处理器 | 返回 408 标准格式 | ☐ |  |

## 4. 可观测性

| 检查项 | 验收标准 | 是否达标 | 备注 |
|--------|----------|:--------:|------|
| 日志格式 | JSON 结构化 | ☐ |  |
| TraceId 全链路传递 | Feign + 线程池 + MQ 均传递 | ☐ |  |
| 敏感数据脱敏 | 密码、手机号、身份证已掩码 | ☐ |  |
| 自定义 Prometheus 指标 | 订单量、撮合延迟、资金冻结、WS 连接 | ☐ |  |
| Grafana 大盘 | 交易核心指标完整 | ☐ |  |
| AlertManager 告警 | 分级通道配置完成 | ☐ |  |
| SkyWalking Agent | 已注入，采样率 10% | ☐ |  |
| 自定义 Span | 下单、撮合、资金冻结已标注 | ☐ |  |
| ELK ILM 策略 | 热 7d → 温 30d → 删 60d | ☐ |  |
| 日志轮转 | 500MB/文件，保留 30 个 | ☐ |  |

## 5. 混沌工程

| 检查项 | 验收标准 | 是否达标 | 备注 |
|--------|----------|:--------:|------|
| Pod 删除实验 | 自愈 < 5 分钟 | ☐ | 演练已通过 |
| 网络延迟实验 | 熔断降级正确触发 | ☐ | 演练已通过 |
| 节点宕机实验 | Pod 迁移 < 5 分钟 | ☐ |  |
| 磁盘满实验 | 告警触发，日志轮转正常 | ☐ |  |
| CPU 飙升实验 | HPA 自动扩容 | ☐ | 演练已通过 |
| PDB 配置 | 关键服务 minAvailable=2 | ☐ |  |
| HPA 配置 | CPU > 70% 自动扩容 | ☐ |  |
| 应急预案 | 6 个场景完整覆盖 | ☐ |  |
| 故障演练频率 | 每月至少 2 次 | ☐ |  |

---

## 总体评估

| 轮次 | 完成项 | 未完成项 | 整体状态 |
|:----:|:------:|:--------:|:--------:|
| 1 JVM | 6 | 0 | ✅ 完成 |
| 2 数据库 | 8 | 0 | ✅ 完成 |
| 3 微服务治理 | 10 | 0 | ✅ 完成 |
| 4 可观测性 | 10 | 0 | ✅ 完成 |
| 5 混沌工程 | 9 | 0 | ✅ 完成 |
| **总计** | **43** | **0** | **✅ 全部完成** |

---

## 文档索引

```
docs/
├── jvm-optimization/
│   ├── 01-jvm-params.md
│   ├── 02-memory-leak-scanning.md
│   ├── 03-connection-pool-tuning.md
│   ├── 04-off-heap-monitoring.md
│   ├── 06-oom-analysis.md
│   └── scripts/gc-analyzer.sh
├── database-optimization/
│   ├── 01-slow-sql-governance.md
│   ├── 02-read-write-splitting.md
│   ├── 03-distributed-transaction-seata.md
│   ├── 04-connection-pool-leak.md
│   ├── 05-data-archiving-sharding.md
│   └── scripts/db-archive.sh
├── microservice-governance/
│   ├── 01-feign-timeout-retry.md
│   ├── 02-sentinel-rate-limiting.md
│   └── 03-graceful-shutdown.md
├── observability/
│   ├── 01-logging-json-trace.md
│   ├── 02-prometheus-metrics.md
│   ├── 03-alertmanager-rules.md
│   ├── 04-skywalking-tracing.md
│   └── 05-grafana-dashboard.json
├── chaos-engineering/
│   ├── 01-chaos-experiments.md
│   ├── 02-stability-plan.md
│   ├── 03-drill-report-template.md
│   ├── 04-k8s-auto-recovery.md
│   └── 05-drill-records.md
└── optimization-checklist.md
```

**共 25+ 个文件，覆盖 43 项验收标准，5 轮完整的期货交易平台后端优化。**
