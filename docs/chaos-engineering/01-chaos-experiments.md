# 混沌实验场景设计（Chaos Mesh）

> 工具: Chaos Mesh 2.0+ | 环境: 测试 K8s 集群 (trading-test)
> 周期: 每月至少 2 次演练

---

## 实验 1：Pod 删除实验

**目标**: 验证 K8s Deployment 的自动重建能力，确保业务无中断

```yaml
# experiments/pod-kill.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: order-service-pod-kill
  namespace: trading-test
spec:
  action: pod-kill
  mode: one                          # 每次删除 1 个 Pod
  duration: 30s
  selector:
    namespaces:
      - trading-test
    labelSelectors:
      app: order-service
  scheduler:
    cron: "@every 120s"             # 每 2 分钟触发一次
```

**预期结果**:
- K8s ReplicaSet 在 30s 内拉起新 Pod
- 滚动更新期间 QPS 不降为 0
- readinessProbe 生效，新 Pod 就绪后才接收流量

---

## 实验 2：网络延迟注入

**目标**: 验证 Sentinel 熔断降级 / Feign 超时配置是否生效

```yaml
# experiments/network-delay.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: order-to-db-delay
  namespace: trading-test
spec:
  action: delay
  mode: all
  duration: 3m
  selector:
    namespaces:
      - trading-test
    labelSelectors:
      app: order-service
  delay:
    latency: "500ms"
    correlation: "50"
    jitter: "100ms"
  target:
    selector:
      namespaces:
        - trading-test
      labelSelectors:
        app: mysql
    mode: all
```

**预期结果**:
- order-service 到数据库的 500ms 延迟导致部分查询超时
- Sentinel 检测到错误率 > 20% 后触发熔断
- 熔断后 5 秒进入半开状态，尝试恢复
- 降级返回兜底数据，不影响核心下单流程

---

## 实验 3：节点宕机模拟

**目标**: 验证 Pod 能否在 5 分钟内迁移到其他节点

```yaml
# experiments/node-failure.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: node-shutdown
  namespace: trading-test
spec:
  action: pod-kill
  mode: all
  selector:
    namespaces:
      - trading-test
    labelSelectors:
      app: order-service
      app: matching-service
```

**预期结果**:
- 节点标记为 `NotReady`
- K8s controller-manager 在 5 分钟内将 Pod 调度到健康节点
- PDB 确保关键服务至少有 2 个副本在线

---

## 实验 4：磁盘满模拟

**目标**: 验证日志轮转和告警是否正常工作

```yaml
# experiments/disk-fill.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: order-service-disk-fill
  namespace: trading-test
spec:
  mode: one
  duration: 5m
  selector:
    namespaces:
      - trading-test
    labelSelectors:
      app: order-service
  stressors:
    volume:
      workers: 1
      size: 2GB                 # 创建 2GB 文件填满日志盘
      path: /var/log
```

**预期结果**:
- 磁盘使用率接近 100% 后触发告警（Prometheus node_filesystem_avail_bytes）
- 日志轮转触发，老日志被压缩/删除
- 应用正常写入，不因磁盘满而崩溃

---

## 实验 5：CPU 飙升实验

**目标**: 验证 HPA 自动扩容是否生效

```yaml
# experiments/cpu-stress.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: matching-service-cpu-burn
  namespace: trading-test
spec:
  mode: one
  duration: 10m
  selector:
    namespaces:
      - trading-test
    labelSelectors:
      app: matching-service
  stressors:
    cpu:
      workers: 4
      load: 80                  # CPU 使用率上升到 80%
```

**预期结果**:
- matching-service Pod CPU 飙升至 80%+
- HPA 检测到 CPU > 70% 持续 3 分钟
- HPA 将副本数从 2 扩展到 4
- 流量分布到新 Pod 后，CPU 回落

---

## 实验执行步骤

每次混沌实验遵循以下 SOP：

```bash
# 1. 告知团队（钉钉通知: 立即开始混沌实验）
kubectl apply -f experiments/pod-kill.yaml

# 2. 观察实时指标（Grafana 大盘 + K8s 事件）
kubectl get pods -n trading-test -w
kubectl describe pod <affected-pod>

# 3. 验证业务是否中断
# 持续发送测试订单
curl -X POST http://test-gateway/api/order/place -d '{"userId":1,"symbol":"HSI2309","side":"BUY","type":"LIMIT","quantity":1,"price":20000}'

# 4. 记录实验数据
# 记录: 中断时长、错误数、自愈时间

# 5. 清理实验
kubectl delete -f experiments/pod-kill.yaml

# 6. 生成演练报告
```
