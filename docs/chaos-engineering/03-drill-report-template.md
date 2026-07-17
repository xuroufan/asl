# 故障演练报告

> 每次混沌实验后填写，存档至 docs/chaos-engineering/reports/

---

## 演练基本信息

| 字段 | 内容 |
|------|------|
| **演练编号** | FT-2026-0XX |
| **演练日期** | 2026-XX-XX |
| **演练名称** | XXX |
| **负责人** | XXX |
| **参与人员** | XXX |
| **实验类型** | Pod 删除 / 网络延迟 / 节点宕机 / 磁盘满 / CPU 飙升 |
| **环境** | trading-test K8s 集群 |
| **持续时间** | XX 分钟 |

---

## 实验目标

<!-- 描述本次实验验证的能力 -->

1. 验证 ...
2. 验证 ...

---

## 执行步骤

### Step 1: 基线确认

```bash
# 记录实验前的状态
kubectl get pods -n trading-test
# Pod 数量: XX
# 正常 Pod 数量: XX
```

### Step 2: 注入故障

```bash
# 执行命令
kubectl apply -f experiments/xxx.yaml
```

| 时间 | 事件 |
|:----:|------|
| T+0s | 故障注入 |
| T+XXs | 首次观测到影响 |
| T+XXs | 自动恢复完成 |

### Step 3: 观测记录

**K8s 事件**:
```
LAST SEEN   TYPE      REASON      OBJECT                 MESSAGE
XXs         Normal    Killing     pod/order-service-xxx   Stopping container
XXs         Normal    Pulling     pod/order-service-xxx   Pulling image
XXs         Normal    Started     pod/order-service-xxx   Started container
```

**业务影响**:
| 指标 | 基线 | 故障期间 | 恢复后 |
|------|:----:|:--------:|:------:|
| 下单 QPS | XXX | XXX | XXX |
| 错误率 | X% | X% | X% |
| P99 延迟 | XXms | XXms | XXms |
| 可用 Pod 数 | XX | XX | XX |
| 自愈时间 | — | XXs | — |

---

## 结果分析

### 是否符合预期？

- [ ] 完全符合预期
- [ ] 部分符合（见下方说明）
- [ ] 不符合预期

### 发现的问题

1. **问题描述**: ...
   **原因分析**: ...
   **改进措施**: ...

2. **问题描述**: ...

### 改进 Action Items

| # | 改进项 | 负责人 | 截止日期 | 状态 |
|:-:|--------|:------:|:--------:|:----:|
| 1 | ... | ... | ... | 待处理 |
| 2 | ... | ... | ... | 待处理 |

---

## 附件

- 故障期间 Grafana 截图: [链接]
- 故障期间日志片段: [链接]
- K8s 事件导出: [链接]

---

## 审批

| 角色 | 姓名 | 签名 | 日期 |
|------|:----:|:----:|:----:|
| 演练负责人 | | | |
| SRE 主管 | | | |
