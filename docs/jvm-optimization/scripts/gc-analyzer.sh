#!/bin/bash
# ============================================================
# GC 日志分析脚本
# 适用日志格式: -Xlog:gc*:file=<path>:time,uptime,level,tags
# 快速定位 GC 频繁原因
# 兼容 macOS / Linux
# ============================================================
# 用法:
#   ./gc-analyzer.sh /path/to/app-gc.log
#   ./gc-analyzer.sh /path/to/gc-*.log.0  (轮转后的文件)
# ============================================================

set -euo pipefail

GC_LOG="${1:?Usage: $0 <gc-log-file>}"
[ ! -f "$GC_LOG" ] && echo "错误: 文件不存在: $GC_LOG" && exit 1

SERVICE_NAME="${2:-unknown}"

# 提取 ms 数值的辅助函数 (兼容 macOS grep, 无 -P)
extract_ms() {
    sed -n 's/.* \([0-9.]*\)ms.*/\1/p'
}

echo "========================================="
echo " GC 日志分析报告"
echo " 服务:       $SERVICE_NAME"
echo " 日志文件:   $GC_LOG"
echo " 分析时间:   $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

# ---------- 基本信息 ----------
TOTAL_LINES=$(wc -l < "$GC_LOG" | tr -d ' ')
echo "[基本信息]"
echo "  日志行数: $TOTAL_LINES"

# ---------- GC 事件统计 ----------
echo ""
echo "[GC 事件统计]"

safe_grep_count() {
    # grep -c 在无匹配时退出码为 1 但 stdout 已经是 "0"
    grep -c "$1" "$GC_LOG" 2>/dev/null || true
}

YOUNG_COUNT=$(safe_grep_count "Pause Young")
FULL_GC_COUNT=$(safe_grep_count "Pause Full")
MIXED_COUNT=$(safe_grep_count "Pause Young (Mixed)")
CONC_COUNT=$(safe_grep_count "Concurrent")
HUMONGOUS_COUNT=$(safe_grep_count "Humongous")
CMF_COUNT=$(safe_grep_count "Concurrent Mode Failure")
TSE_COUNT=$(safe_grep_count "to-space")

echo "  G1 Young GC:     $YOUNG_COUNT 次"
echo "  G1 Full GC:      $FULL_GC_COUNT 次  <-- 应趋近于0"
echo "  Mixed GC:        $MIXED_COUNT 次"
echo "  G1 Concurrent:   $CONC_COUNT 次"

# ---------- GC 停顿时间统计 ----------
echo ""
echo "[GC 停顿时间分析] (单位: ms)"

if [ "$YOUNG_COUNT" -gt 0 ]; then
    grep "Pause Young" "$GC_LOG" | extract_ms | awk '
    BEGIN { min=1e9; max=0; sum=0; cnt=0 }
    /^[0-9]/ { v=$1+0; if(v<min)min=v; if(v>max)max=v; sum+=v; cnt++ }
    END {
        if(cnt>0) {
            printf "  Young GC 暂停 (%d 次):\n", cnt
            printf "    最小:   %.2f ms\n", min
            printf "    最大:   %.2f ms\n", max
            printf "    平均:   %.2f ms\n", sum/cnt
        }
    }'
fi

if [ "$FULL_GC_COUNT" -gt 0 ]; then
    grep -E "Pause Full" "$GC_LOG" | extract_ms | awk '
    BEGIN { min=1e9; max=0; sum=0; cnt=0 }
    /^[0-9]/ { v=$1+0; if(v<min)min=v; if(v>max)max=v; sum+=v; cnt++ }
    END {
        if(cnt>0) {
            printf "  Full GC 暂停 (%d 次):\n", cnt
            printf "    最小:   %.2f ms\n", min
            printf "    最大:   %.2f ms\n", max
            printf "    平均:   %.2f ms\n", sum/cnt
        }
    }'
fi

# ---------- 大对象分配 (Humongous) ----------
echo ""
echo "[大对象分配统计]"
echo "  Humongous 分配次数: $HUMONGOUS_COUNT"
if [ "$HUMONGOUS_COUNT" -gt 100 ]; then
    echo "  [警告] 大对象分配频繁! 检查代码中 >1MB 的 byte[]"
fi

# ---------- Concurrent Mode Failure ----------
echo ""
echo "[Concurrent Mode Failure]"
if [ "$CMF_COUNT" -gt 0 ]; then
    echo "  出现次数: $CMF_COUNT"
    echo "  [严重] 并发回收跟不上分配速率，退化为 Full GC"
    echo "  建议: 降低 IHOP 到 35-40, 或增大堆"
else
    echo "  未检测到"
fi

# ---------- To-space Exhausted ----------
echo ""
echo "[To-space Exhausted]"
if [ "$TSE_COUNT" -gt 0 ]; then
    echo "  出现次数: $TSE_COUNT"
    echo "  [严重] G1 回收空间不足，退化为 Full GC"
    echo "  建议: 增大 G1ReservePercent 到 25"
else
    echo "  未检测到"
fi

# ---------- 综合结论 ----------
echo ""
echo "========================================="
echo "[综合结论]"

if [ "$FULL_GC_COUNT" -gt 0 ]; then
    echo "  服务 $SERVICE_NAME 发生了 $FULL_GC_COUNT 次 Full GC"
    echo "  可能导致秒级暂停，影响订单处理延迟"
    echo "  立即 jmap -histo:live 和 MAT 分析"
elif [ "$CMF_COUNT" -gt 0 ]; then
    echo "  服务 $SERVICE_NAME 存在 Concurrent Mode Failure"
    echo "  需要调整 IHOP 或增大堆"
elif [ "$HUMONGOUS_COUNT" -gt 500 ]; then
    echo "  服务 $SERVICE_NAME 大对象分配频繁"
    echo "  建议检查大 byte[] 和序列化类"
elif [ "$YOUNG_COUNT" -gt 1000 ]; then
    echo "  服务 $SERVICE_NAME Young GC 较频繁 (>1000次)"
    echo "  建议增大年轻代或检查对象创建速率"
else
    echo "  服务 $SERVICE_NAME GC 状况基本健康"
fi
echo "========================================="
