#!/bin/bash
# ============================================================
# Futures Platform - 负载测试脚本
# 使用 ab (Apache Bench) 进行 HTTP 性能测试
# ============================================================
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8088}"
REQUESTS="${TOTAL_REQUESTS:-1000}"
CONCURRENCY="${CONCURRENT:-10}"
RESULTS_DIR="load-test-results"

mkdir -p "$RESULTS_DIR"
echo "=========================================="
echo "  Futures Platform - 负载测试"
echo "  Gateway: $GATEWAY"
echo "  总请求: $REQUESTS"
echo "  并发数: $CONCURRENCY"
echo "=========================================="

run_test() {
    local name="$1"
    local url="$2"
    local file="$RESULTS_DIR/${name//\//_}.txt"
    shift 2
    
    echo ""
    echo "--- $name ---"
    ab -n "$REQUESTS" -c "$CONCURRENCY" "$@" \
       -g "${file%.txt}.dat" \
       -e "${file%.txt}.csv" \
       "$url" 2>&1 | tee "$file" | grep -E "Requests per second|Time per request|Failed requests|Transfer rate|Percentage of the requests"
}

# 1. 行情API（公开）
run_test "market-symbols" "$GATEWAY/api/v1/market/symbols"

# 2. 注册（先注册一个测试用户）
TS=$(date +%s)
echo ""
echo "--- Preparing test user ---"
curl -s -X POST "$GATEWAY/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"load_${TS}\",\"password\":\"Test1234!\",\"email\":\"load_${TS}@test.com\"}" > /dev/null

LOGIN_RESP=$(curl -s -X POST "$GATEWAY/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"load_${TS}\",\"password\":\"Test1234!\"}")
USER_ID=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('userInfo',{}).get('userId',''))" 2>/dev/null || echo "")

# 3. 登录（POST 请求）
run_test "auth-login" "$GATEWAY/api/v1/auth/login" \
  -T "application/json" \
  -p <(echo "{\"username\":\"load_${TS}\",\"password\":\"Test1234!\"}")

# 4. 资金查询（GET + Bearer token）
if [ -n "$USER_ID" ]; then
    TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
    if [ -n "$TOKEN" ]; then
        run_test "fund-balance" "$GATEWAY/api/v1/fund/balance?userId=$USER_ID" \
          -H "Authorization: Bearer $TOKEN"
        
        # 5. 入金（POST + Bearer token）
        run_test "fund-deposit" "$GATEWAY/api/v1/fund/deposit" \
          -T "application/json" \
          -H "Authorization: Bearer $TOKEN" \
          -p <(echo "{\"userId\":\"$USER_ID\",\"amount\":1000}")
    fi
fi

echo ""
echo "=========================================="
echo "  负载测试完成"
echo "  报告: $(pwd)/$RESULTS_DIR/"
echo "=========================================="
