# 故障演练执行记录

> 持续更新，每次演练后追加

---

## 演练记录 1：Pod 删除实验

| 字段 | 内容 |
|------|------|
| **演练编号** | FT-2026-001 |
| **日期** | 2026-07-10 |
| **演练名称** | order-service Pod 删除恢复 |
| **负责人** | 张三 (SRE) |
| **实验类型** | Pod 删除 |
| **环境** | trading-test |
| **持续时间** | 5 分钟 |

### Step 1: 基线确认

```bash
$ kubectl get pods -n trading-test -l app=order-service
NAME                              READY   STATUS    RESTARTS   AGE
order-service-6b8d47d8f7-abc12   1/1     Running   0          12h
order-service-6b8d47d8f7-def34   1/1     Running   0          12h
order-service-6b8d47d8f7-ghi56   1/1     Running   0          12h

$ kubectl get pdb order-service-pdb
NAME                  MIN AVAILABLE   MAX UNAVAILABLE   ALLOWED DISRUPTIONS   AGE
order-service-pdb     2               N/A               1                     7d
```

### Step 2: 注入故障

```bash
$ kubectl delete pod order-service-6b8d47d8f7-abc12
pod "order-service-6b8d47d8f7-abc12" deleted
```

### Step 3: 观测记录

**时间线**:
| 时间 | 事件 |
|:----:|------|
| T+0s | Pod 删除命令执行 |
| T+1s | Pod 状态变为 Terminating |
| T+3s | Deployment 创建新 Pod |
| T+10s | 新 Pod 容器启动 |
| T+22s | readinessProbe 通过，新 Pod 进入 Ready 状态 |
| T+25s | 完全恢复 |

**K8s 事件**:
```
10s  Normal  Killing     pod/order-service-6b8d47d8f7-abc12  Stopping container
3s   Normal  SuccessfulCreate  replicaset/order-service-6b8d47d8f7  Created pod: order-service-6b8d47d8f7-jkl56
22s  Normal  Readiness   pod/order-service-6b8d47d8f7-jkl56  Readiness probe passed
```

**业务影响**:
| 指标 | 基线 | 故障期间 | 恢复后 |
|------|:----:|:--------:|:------:|
| 下单 QPS | 120 | 118 | 121 |
| 错误率 | 0% | 0% | 0% |
| P99 延迟 | 45ms | 52ms | 44ms |
| 可用 Pod 数 | 3 | 2 (PDB 生效) | 3 |

### 结论

- [x] 完全符合预期
- PDB 阻止了同时删除多个 Pod（minAvailable=2 生效）
- 自愈时间: **25 秒**
- 业务零中断

---

## 演练记录 2：网络延迟实验

| 字段 | 内容 |
|------|------|
| **演练编号** | FT-2026-002 |
| **日期** | 2026-07-14 |
| **演练名称** | order-service → MySQL 500ms 延迟 |
| **负责人** | 李四 (SRE) |
| **实验类型** | 网络延迟 |
| **环境** | trading-test |
| **持续时间** | 8 分钟 |

### Step 1: 基线确认

```bash
# 确认熔断配置
$ curl http://sentinel-dashboard:8080/api/v1/flow/rules
# order:place 限流 50 QPS, 错误率熔断 20%, 半开 5s

# 启动持续监控
$ kubectl exec -it load-generator -- sh -c "while true; do curl -s -o /dev/null -w '%{http_code}\n' http://gateway:8080/api/order/place -X POST -d '{}'; sleep 0.1; done"
```

### Step 2: 注入故障

```bash
$ kubectl apply -f experiments/network-delay.yaml
```

### Step 3: 观测记录

**时间线**:
| 时间 | 事件 |
|:----:|------|
| T+0s | 网络延迟注入 500ms |
| T+10s | 部分查询超时（Feign readTimeout=5s 生效） |
| T+30s | 错误率超过 20%，Sentinel 触发熔断 |
| T+35s | 降级返回兜底数据 |
| T+185s | 熔断 5 秒后进入半开状态 |
| T+190s | 半开成功，恢复正常 |
| T+200s | Chaos Mesh 实验自动结束 |

**Sentinel 控制台指标**:
```
熔断前:
  - 通过 QPS: 43
  - 拒绝 QPS: 0
  - 错误比例: 0%

熔断中:
  - 通过 QPS: 3 (降级返回)
  - 拒绝 QPS: 40 (熔断快速失败)
  - 错误比例: 0% (熔断后不再调用)

半开恢复后:
  - 通过 QPS: 45
  - 拒绝 QPS: 0
  - 错误比例: 0%
```

### 发现的问题

1. **问题**: 部分 Feign 短连接（connectTimeout=1s）在网络延迟下频繁超时
   **改进**: 对短暂网络抖动增加重试（Retryer），但对非幂等下单操作仍保留不重试

2. **问题**: 熔断恢复后，延迟指标显示部分连接仍在等待旧请求完成
   **改进**: 增加 Tomcat 连接超时配置，确保请求不会无限等待

### 结论

- [x] 完全符合预期
- Sentinel 熔断降级在 30 秒内正确触发
- 熔断 5 秒后半开自动恢复
- 降级期间用户收到 429 友好提示
- 实验验证了网络延迟场景的防御能力

---

## 演练记录 3：CPU 飙升实验

| 字段 | 内容 |
|------|------|
| **演练编号** | FT-2026-003 |
| **日期** | 2026-07-16 |
| **演练名称** | matching-service CPU 80% + HPA 扩容 |
| **负责人** | 王五 (SRE) |
| **实验类型** | CPU 飙升 |
| **环境** | trading-test |
| **持续时间** | 15 分钟 |

### Step 1: 基线确认

```bash
$ kubectl get hpa matching-service-hpa
NAME                    REFERENCE                      TARGETS   MINPODS   MAXPODS   REPLICAS
matching-service-hpa    Deployment/matching-service    35%/60%   2         8         2
```

### Step 2: 注入故障

```bash
$ kubectl apply -f experiments/cpu-stress.yaml
```

### Step 3: 观测记录

**时间线**:
| 时间 | 事件 |
|:----:|------|
| T+0s | StressChaos 注入，CPU 升到 80% |
| T+30s | HPA 开始观测到 CPU 升高 |
| T+90s | HPA 稳定窗口结束，启动扩容（2→3） |
| T+150s | 继续扩容（3→4） |
| T+180s | 新 Pod 就绪，CPU 开始回落 |
| T+300s | CPU 降回 40%，扩容停止 |
| T+600s | StressChaos 结束，CPU 恢复至基线 |

### 结论

- [x] 完全符合预期
- HPA 在 2 分钟内完成首次扩容（从 2 扩到 3）
- 最终稳定在 4 个副本，CPU 控制在 60% 以下
- 扩容期间业务无中断
