# 6. OOM 堆转储分析步骤

## 6.1 OOM 触发时的自动行为

配置生效后，JVM OOM 时自动生成：
```
堆转储: /app/logs/heapdump.hprof
错误日志: /app/logs/hs_err_pid<pid>.log
GC 日志: /app/logs/gc.log → 保留最近10个100MB文件
```

K8s 环境下，Pod 退出后需将堆转储备份到持久卷：

```yaml
# K8s volume 挂载示例
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: matching
        volumeMounts:
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: logs
        hostPath:
          path: /data/futures-logs/$(POD_NAME)
          type: DirectoryOrCreate
```

## 6.2 离线分析步骤 (Eclipse MAT)

### 第一步：加载堆转储

```bash
# 在本地分析机上
# 1. 从 K8s Pod 获取堆转储
kubectl cp futures-matching-xxxxx:/app/logs/heapdump.hprof ./heapdump-$DATE.hprof

# 2. 用 MAT 打开（建议 8GB+ 堆内存）
/Applications/mat.app/Contents/MacOS/MemoryAnalyzer \
    -vmargs -Xmx8g \
    -data ./mat-workspace \
    ./heapdump-$DATE.hprof
```

### 第二步：MAT 分析流程

```
1. Leak Suspects Report（泄漏嫌疑报告）
   → 查看最大的保留集（Retained Set）
   → 通常是 char[], String, HashMap$Node

2. Dominator Tree（支配树）
   → 按 Retained Heap 降序排列
   → 查找最大的对象

3. Thread Overview（线程概览）
   → 检查每个线程的栈帧和局部变量
   → 查找 ThreadLocal 持有的大对象

4. OQL 查询（特定问题诊断）
```

### 第三步：OQL 诊断查询

```sql
-- 1. 查找最大的 char[]（重复字符串导致）
SELECT * FROM char[] ORDER BY BYTES(object) DESC LIMIT 20

-- 2. 查找所有 ThreadLocal 值
SELECT * FROM java.lang.ThreadLocal$ThreadLocalMap$Entry

-- 3. 查找 HashMap 中最多的条目
SELECT s, s.elementData.length 
FROM java.util.HashMap s 
ORDER BY s.elementData.length DESC LIMIT 10

-- 4. 查找 String 去重后可节省的空间
SELECT toString(s), s.count, s.value.length 
FROM java.lang.String s 
ORDER BY s.value.length DESC LIMIT 100

-- 5. 查找 Stop Order 相关对象
SELECT * FROM com.futures.matching.model.Order 
WHERE price > 0 AND filledVolume < volume
```

### 第四步：常见 OOM 类型判断

| OOM 类型 | 堆转储特征 | 根因分析方向 |
|----------|-----------|------------|
| **Java Heap Space** | char[] 或 byte[] 占用 > 80% | 字符串/缓存泄漏 |
| **Metaspace** | Class 加载器泄漏 | 热部署/CGLIB 代理 |
| **Direct Memory** | 无法通过堆转储直接分析 | 查看 `hs_err_pid.log`，检查 Netty |
| **GC Overhead Limit** | 大量待回收对象 | G1GC 调优或内存分配速率过高 |

## 6.3 快速定位脚本

```bash
#!/bin/bash
# oom-analyze.sh — OOM 堆转储快速分析脚本
# 依赖: jhat 或 Eclipse MAT headless

HPROF_FILE=$1
if [ -z "$HPROF_FILE" ]; then
    echo "Usage: $0 <heap-dump.hprof>"
    exit 1
fi

echo "=== OOM 堆转储分析 ==="
echo "文件: $HPROF_FILE"
echo "大小: $(ls -lh "$HPROF_FILE" | awk '{print $5}')"
echo ""

# 1. 基本信息
echo "=== 1. 堆基本信息 ==="
jhat -parse-only "$HPROF_FILE" 2>&1 | head -20 || echo "jhat not available"

# 2. 类实例统计（用 jmap 替代）
echo ""
echo "=== 2. 类实例 Top 30 ==="
# jhat 不可用时，用 MAT headless
which ParseHeapDump.sh 2>/dev/null && ParseHeapDump.sh "$HPROF_FILE" \
    org.eclipse.mat.api:top_components

echo ""
echo "=== 3. 分析建议 ==="
echo "  推荐使用 Eclipse MAT GUI 进行完整分析"
echo "  重点关注:"
echo "    - Leak Suspects Report"
echo "    - Dominator Tree"
echo "    - GC Roots 路径"
```

## 6.4 OOM 预防措施

```yaml
# 生产环境必须配置：
-XX:+HeapDumpOnOutOfMemoryError    # ✅ 当前已配置
-XX:HeapDumpPath=/app/logs/       # ✅ 当前已配置
-XX:+ExitOnOutOfMemoryError        # ✅ 新增（自动退出，由K8s重启）
-Xlog:gc*:file=/app/logs/gc.log    # ✅ 当前已配置，但建议增大单文件大小至100M
```

## 6.5 各服务 OOM 风险评级

| 服务 | 风险等级 | 主要风险 | 缓解措施 |
|------|---------|---------|---------|
| `futures-matching` | 🔴**高** | 止损单未清理 / 订单簿深 | 止损单TTL / 订单簿深度限制 |
| `futures-order` | 🟠中 | 订单数据累积 / 缓存未清理 | 分页查询 / 缓存TTL |
| `futures-market` | 🟠中 | K线数据在内存中聚合 | 限制内存窗口周期 |
| `futures-gateway` | 🟠中 | Netty Buffer 管理 | 监控Direct Memory |
| `futures-push` | 🟡低 | WebSocket 连接持有 | 连接数上限 |
| `futures-account` | 🟡低 | 标准 CRUD | 常规架构 |
| `futures-risk` | 🟡低 | 风控规则在内存 | 规则数有限 |
| `futures-fund` | 🟡低 | 标准 CRUD | 常规架构 |
| `futures-settlement` | 🟡低 | 日终批量处理 | 分批处理 |
| `futures-admin` | 🟡低 | 标准 CRUD | 常规架构 |
