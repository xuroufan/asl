# 5. GC 日志分析脚本

## 5.1 GC 日志快速诊断脚本

```bash
#!/bin/bash
# gc-analyze.sh — 快速定位 GC 频繁原因
# Usage: ./gc-analyze.sh /var/log/app-gc.log

GC_LOG=$1
if [ -z "$GC_LOG" ]; then
    echo "Usage: $0 <gc-log-file>"
    exit 1
fi

echo "=========================================="
echo " GC 日志分析报告"
echo " 文件: $GC_LOG"
echo "=========================================="

# 1. 基本信息
echo ""
echo "=== 1. GC 整体统计 ==="
TOTAL_GC=$(grep -c "Pause Young" "$GC_LOG" 2>/dev/null)
FULL_GC=$(grep -c "Pause Full" "$GC_LOG" 2>/dev/null)
echo "  Young GC 次数: $TOTAL_GC"
echo "  Full GC 次数: $FULL_GC"

# 2. GC 耗时分布
echo ""
echo "=== 2. GC 暂停时间统计 (ms) ==="
awk '/Pause Young/ {
    match($0, /([0-9.]+)ms/, arr);
    if (arr[1] != "") {
        sum += arr[1]; count++;
        if (arr[1] > max) max = arr[1];
        if (min == 0 || arr[1] < min) min = arr[1];
        if (arr[1] > 200) slow++;
    }
} END {
    if (count > 0) {
        printf "  Young GC 次数: %d\n", count;
        printf "  平均暂停: %.2f ms\n", sum/count;
        printf "  最短暂停: %.2f ms\n", min;
        printf "  最长暂停: %.2f ms\n", max;
        printf "  超过200ms次数: %d (%.1f%%)\n", slow, (slow/count)*100;
    }
}' "$GC_LOG"

echo ""
awk '/Pause Full/ {
    match($0, /([0-9.]+)ms/, arr);
    if (arr[1] != "") {
        fsum += arr[1]; fcount++;
        if (arr[1] > fmax) fmax = arr[1];
    }
} END {
    if (fcount > 0) {
        printf "  Full GC 次数: %d\n", fcount;
        printf "  平均暂停: %.2f ms\n", fsum/fcount;
        printf "  最长暂停: %.2f ms\n", fmax;
    }
}' "$GC_LOG"

# 3. GC 频率
echo ""
echo "=== 3. GC 频率分析 ==="
START_TIME=$(head -1 "$GC_LOG" | grep -oP '\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}' || echo "unknown")
END_TIME=$(tail -1 "$GC_LOG" | grep -oP '\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}' || echo "unknown")
echo "  日志起始: $START_TIME"
echo "  日志结束: $END_TIME"

# 4. GC 前后堆内存变化
echo ""
echo "=== 4. GC 前后堆内存变化 (最近20次) ==="
grep "Pause Young\|Pause Full" "$GC_LOG" | tail -20 | while read line; do
    # 提取 GC 前后内存
    BEFORE=$(echo "$line" | grep -oP '\d+[KMG]?->\d+[KMG]?' | cut -d'>' -f1)
    AFTER=$(echo "$line" | grep -oP '->\d+[KMG]?' | cut -d'>' -f2)
    echo "  $BEFORE → $AFTER"
done

# 5. 晋升到老年代的对象
echo ""
echo "=== 5. 晋升到老年代的对象大小 ==="
grep "Pause Young" "$GC_LOG" | grep -oP '\d+[KMG]?->\d+[KMG]?\(\d+[KMG]?\)' | head -20

# 6. 建议
echo ""
echo "=== 6. 优化建议 ==="
if [ $FULL_GC -gt 0 ]; then
    echo "  ⚠️  存在 Full GC! 建议:"
    echo "    - 检查 -XX:InitiatingHeapOccupancyPercent 是否过低"
    echo "    - 检查是否存在大对象直接进入老年代"
    echo "    - 使用 jmap -histo:live <pid> 查看对象分布"
fi
if [ $TOTAL_GC -gt 1000 ]; then
    echo "  ⚠️  Young GC 次数过多 ($TOTAL_GC次)! 建议:"
    echo "    - 增大 -Xmn 或 -XX:G1NewSizePercent"
    echo "    - 检查是否有大量短生命周期对象"
fi
```

## 5.2 实时 GC 监控（生产环境）

```bash
#!/bin/bash
# gc-monitor.sh — 实时监控 GC 情况
# 用法: 在容器内运行

PID=$1
INTERVAL=${2:-5}

echo "Monitoring GC for PID $PID every ${INTERVAL}s..."
echo ""

while true; do
    jstat -gcutil $PID 1000 1 | tail -1 | awk '{
        printf "[%s] ", strftime("%H:%M:%S");
        printf "S0:%.1f%% S1:%.1f%% E:%.1f%% O:%.1f%% M:%.1f%% YGC:%d YGCT:%.2fs FGC:%d FGCT:%.2fs\n",
        $1, $2, $3, $4, $5, $7, $6, $9, $8;
    }'
    sleep $INTERVAL
done
```

## 5.3 GC 优化检查清单

| 检查项 | 命令 | 正常阈值 |
|--------|------|---------|
| Young GC 频率 | `jstat -gc <pid> 1000` | < 5次/秒 |
| Full GC 频率 | `jstat -gcutil <pid>` | 0次/小时 |
| GC 暂停时间 | GC 日志 `*.ms` | 平均 < 100ms |
| 老年代占用率 | `jstat -gcutil` O 列 | < 70% |
| 元空间占用率 | `jstat -gcutil` M 列 | < 90% |
| Direct Memory | Prometheus 指标 | < 512MB |
