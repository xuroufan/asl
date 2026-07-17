# 自动恢复配置（K8s PDB + HPA）

> 确保关键服务在故障时自动恢复，不依赖人工介入

---

## 1. PodDisruptionBudget (PDB)

确保关键服务在节点故障或滚动更新时至少有 2 个副本在线：

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: order-service-pdb
  namespace: trading-backend
spec:
  minAvailable: 2                # 至少有 2 个 Pod 可用
  selector:
    matchLabels:
      app: order-service
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: matching-service-pdb
  namespace: trading-backend
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: matching-service
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: fund-service-pdb
  namespace: trading-backend
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: fund-service
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: gateway-service-pdb
  namespace: trading-backend
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: gateway-service
```

---

## 2. HorizontalPodAutoscaler (HPA)

基于 CPU 和内存的自动扩缩容：

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: trading-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 120     # 120 秒稳定窗口
      policies:
        - type: Pods
          value: 2                        # 每 60 秒最多扩容 2 个 Pod
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300     # 缩容等待 5 分钟
      policies:
        - type: Pods
          value: 1                        # 每 60 秒最多缩容 1 个
          periodSeconds: 60
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: matching-service-hpa
  namespace: trading-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: matching-service
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 30
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gateway-service-hpa
  namespace: trading-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
```

---

## 3. 自动恢复能力验证清单

### 3.1 Pod 自动重建

```bash
# 验证命令
kubectl delete pod -l app=order-service
kubectl get pods -l app=order-service -w
# 预期: 30 秒内新 Pod 进入 Running 状态
```

### 3.2 PDB 生效

```bash
# 验证 PDB
kubectl get pdb order-service-pdb
# 预期: ALLOWED DISRUPTIONS = 1 或更多

# 同时删除 2 个 Pod
kubectl delete pod order-service-xxx order-service-yyy
# 预期: PDB 阻止第二次删除
```

### 3.3 HPA 自动扩容

```bash
# 模拟高负载
kubectl run load-generator --image=busybox -- /bin/sh -c "while true; do wget -q -O- http://order-service:8080/api/order/history; done"

# 观察 HPA
kubectl get hpa order-service-hpa -w
# 预期: CPU > 70% 后 2-3 分钟内 Pod 数量增加
```

### 3.4 滚动更新不中断

```bash
# 启动持续请求
while true; do curl -s http://gateway:8080/api/order/history | head -c 20; echo; sleep 0.5; done

# 触发滚动更新
kubectl rollout restart deployment order-service

# 预期:
# - 请求一直成功（无 Connection Refused）
# - 更新完成后所有 Pod 为新版本
```

---

## 4. 各服务自动恢复配置汇总

| 服务 | 最小副本 | 最大副本 | HPA 阈值 | PDB minAvailable | 说明 |
|------|:--------:|:--------:|:--------:|:----------------:|------|
| order-service | 3 | 10 | CPU 70% | 2 | 核心服务，保障高可用 |
| matching-service | 2 | 8 | CPU 60% | 2 | 撮合引擎，避免数据丢失 |
| fund-service | 2 | 6 | CPU 70% | 1 | 资金操作，一致性优先 |
| market-service | 2 | 6 | CPU 70% | 1 | 行情服务，可降级 |
| gateway-service | 2 | 20 | CPU 70% | 2 | 入口网关，最大弹性 |
| risk-service | 2 | 4 | CPU 70% | 1 | 风控服务，轻量 |
