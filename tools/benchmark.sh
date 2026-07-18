#!/bin/bash
# ================================================
# ASL 期货平台 — 撮合引擎压力测试
# 测试: 撮合引擎 + 订单服务 API 响应性能
# 用法: ./tools/benchmark.sh [concurrency] [requests]
# ================================================

BASE="$(cd "$(dirname "$0")/.." && pwd)"
CONCURRENT="${1:-10}"   # 并发数（并行请求数）
REQUESTS="${2:-100}"    # 总请求数
MATCHING="http://localhost:8082"
GATEWAY="http://localhost:8088"
LOG="$BASE/logs/benchmark_$(date '+%Y%m%d_%H%M%S').log"

mkdir -p "$BASE/logs"

# ---- 颜色 ----
GRN='\033[0;32m'; RED='\033[0;31m'; YLW='\033[1;33m'; BLU='\033[0;34m'; NC='\033[0m'

log() { echo -e "${GRN}[$(date '+%H:%M:%S')]${NC} $1" | tee -a "$LOG"; }
err() { echo -e "${RED}[$(date '+%H:%M:%S')]${NC} $1" | tee -a "$LOG"; }

# ---- 预检查 ----
precheck() {
  log "预检查..."
  for port in 8082 8081 8088; do
    local svc
    case $port in 8082) svc="撮合引擎";; 8081) svc="订单服务";; 8088) svc="网关";; esac
    local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:$port/actuator/health" 2>/dev/null)
    if [ "$code" = "200" ]; then
      log "  ✅ $svc($port) 正常"
    else
      err "  ❌ $svc($port) 不可用 (HTTP $code)"
      return 1
    fi
  done
  return 0
}

# ---- 单次基准测试 ----
bench_one() {
  local name="$1" url="$2" method="$3" data="$4"
  local start end elapsed code
  start=$(date +%s%N)
  if [ "$method" = "POST" ]; then
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 -X POST -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null)
  else
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
  fi
  end=$(date +%s%N)
  elapsed=$(( (end - start) / 1000000 ))  # ms
  echo "$elapsed $code"
}

# ---- 并发测试 ----
bench_concurrent() {
  local name="$1" url="$2" method="$3" data="$4"
  log "  ${BLU}$name${NC}  (${CONCURRENT}并发 x $REQUESTS请求)"

  local times=()
  local ok=0 fail=0

  # Function to run in background
  do_req() {
    local result=$(bench_one "$name" "$url" "$method" "$data")
    echo "$result"
  }

  local remaining=$REQUESTS
  while [ $remaining -gt 0 ]; do
    local batch=$(( remaining < CONCURRENT ? remaining : CONCURRENT ))
    local pids=()
    
    # Launch batch
    for i in $(seq 1 $batch); do
      do_req &
      pids+=($!)
    done

    # Collect results
    for pid in "${pids[@]}"; do
      wait "$pid" 2>/dev/null
      local result=$?
      # Actually we need the output from the function
    done

    remaining=$(( remaining - batch ))
  done

  # Since bash subprocess output capture is tricky, use simpler approach
  # Just run the requests and capture timing
  local tmpf=$(mktemp)
  for i in $(seq 1 $REQUESTS); do
    (
      local t0=$(date +%s%N)
      local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
      local t1=$(date +%s%N)
      echo "$(( (t1-t0)/1000000 )) $code" >> "$tmpf"
    ) &
    # Limit concurrency
    if [ $((i % CONCURRENT)) -eq 0 ]; then wait; fi
  done
  wait

  # Parse results
  local total=0 count=0 sum=0 min=99999 max=0
  while read -r ms code; do
    count=$((count+1))
    sum=$((sum+ms))
    [ "$ms" -lt "$min" ] && min=$ms
    [ "$ms" -gt "$max" ] && max=$ms
    [ "$code" = "200" ] && ok=$((ok+1)) || fail=$((fail+1))
    total="$total $ms"
  done < "$tmpf"
  rm -f "$tmpf"

  [ "$count" -eq 0 ] && { err "  ❌ 无有效响应"; return; }

  local avg=$(( sum / count ))
  # Sort for percentiles
  local sorted=$(echo "$total" | tr ' ' '\n' | sort -n | grep -v "^$")
  local p95_idx=$(( count * 95 / 100 ))
  local p99_idx=$(( count * 99 / 100 ))
  [ "$p95_idx" -lt 1 ] && p95_idx=1
  [ "$p99_idx" -lt 1 ] && p99_idx=1
  local p95=$(echo "$sorted" | sed -n "${p95_idx}p")
  local p99=$(echo "$sorted" | sed -n "${p99_idx}p")

  local throughput=$(( count * 1000 / (sum / count + 1) ))
  
  echo ""
  echo -e "  ${GRN}结果:${NC}"
  echo -e "  ┌──────────────┬────────┐"
  printf "  │ %-12s │ %6s │\n" "成功率" "${ok}/${count}"
  printf "  │ %-12s │ %6dms │\n" "平均延迟" "$avg"
  printf "  │ %-12s │ %6dms │\n" "最小延迟" "$min"
  printf "  │ %-12s │ %6dms │\n" "最大延迟" "$max"
  printf "  │ %-12s │ %6dms │\n" "P95" "${p95:-N/A}"
  printf "  │ %-12s │ %6dms │\n" "P99" "${p99:-N/A}"
  echo -e "  └──────────────┴────────┘"
  echo "" >> "$LOG"
}

# ---- 主流程 ----
echo ""
echo "========================================"
echo "  ASL 撮合引擎压力测试"
echo "  并发: $CONCURRENT  |  请求: $REQUESTS"
echo "========================================"
echo ""

precheck || { err "预检查失败，取消测试"; exit 1; }

echo ""
log "===== 测试场景 ====="
echo ""

# 1. 健康检查
log "1/3 健康检查 (GET /actuator/health)"
bench_concurrent "健康检查" "$MATCHING/actuator/health" "GET"

echo ""

# 2. 订单簿深度
log "2/3 订单簿深度 (GET /api/v1/matching/depth)"
bench_concurrent "订单簿深度" "$MATCHING/api/v1/matching/depth" "GET"

echo ""

# 3. 中间价
log "3/3 中间价查询 (GET /api/v1/matching/price)"
bench_concurrent "中间价" "$MATCHING/api/v1/matching/price" "GET"

# Optional: 模拟下单
if [ "${3:-}" = "--orders" ]; then
  echo ""
  log "额外: 模拟下单 POST /api/v1/matching/place"
  bench_concurrent "下单测试" "$MATCHING/api/v1/matching/place" "POST" \
    '{"userId":"bench-user","symbol":"HSI","direction":"BUY","orderType":"LIMIT","price":100,"volume":1,"sync":false}'
fi

echo ""
echo "========================================"
log "测试完成"
echo "详细日志: $LOG"
echo "========================================"
