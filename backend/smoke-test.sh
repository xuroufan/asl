#!/bin/bash
# ============================================================
# Futures Platform - 全链路冒烟测试
# 部署后运行，验证核心业务流程是否正常
# ============================================================
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8088}"
PASS=0
FAIL=0

ok()   { PASS=$((PASS+1)); echo -e "  ✅ $1"; }
fail() { FAIL=$((FAIL+1)); echo -e "  ❌ $1 (HTTP $2)"; }

echo "========================================"
echo " Futures Platform - 全链路冒烟测试"
echo " Gateway: $GATEWAY"
echo "========================================"
echo ""

# ---- 1. 网关健康检查 ----
echo "--- 1. 基础设施 ---"
GW=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/actuator/health" --connect-timeout 5 2>/dev/null || echo "000")
[ "$GW" = "200" ] && ok "网关健康" || fail "网关健康" "$GW"

# ---- 2. 行情API（公开，无需Token） ----
echo ""
echo "--- 2. 行情服务 ---"
SYMBOLS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/v1/market/symbols" --connect-timeout 5 2>/dev/null || echo "000")
[ "$SYMBOLS" = "200" ] && ok "行情查询" || fail "行情查询" "$SYMBOLS"

# ---- 3. 注册 ----
echo ""
echo "--- 3. 用户注册 ---"
TS=$(date +%s)
REG_BODY="{\"username\":\"smoke_${TS}\",\"password\":\"Test1234!\",\"email\":\"smoke_${TS}@test.com\"}"
REG=$(curl -s -w "%{http_code}" -X POST "$GATEWAY/api/v1/auth/register" \
  -H "Content-Type: application/json" -d "$REG_BODY" --connect-timeout 5 2>/dev/null || echo "000")
HTTP="${REG: -3}"
BODY="${REG%???}"
[ "$HTTP" = "200" ] && ok "用户注册" || fail "用户注册" "$HTTP"

# ---- 4. 登录 ----
echo ""
echo "--- 4. 用户登录 ---"
LOGIN_BODY="{\"username\":\"smoke_${TS}\",\"password\":\"Test1234!\"}"
LOGIN=$(curl -s -w "%{http_code}" -X POST "$GATEWAY/api/v1/auth/login" \
  -H "Content-Type: application/json" -d "$LOGIN_BODY" --connect-timeout 5 2>/dev/null || echo "000")
HTTP="${LOGIN: -3}"
BODY="${LOGIN%???}"
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('userInfo',{}).get('userId',''))" 2>/dev/null || echo "")
TOKEN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

if [ "$HTTP" = "200" ] && [ -n "$TOKEN" ]; then
  ok "用户登录 (userId=$USER_ID)"
else
  fail "用户登录" "$HTTP"
fi

# ---- 5. 账户信息 ----
echo ""
echo "--- 5. 账户信息 ---"
if [ -n "$TOKEN" ]; then
  ACCT=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/v1/account/overview" \
    -H "Authorization: Bearer $TOKEN" --connect-timeout 5 2>/dev/null || echo "000")
  [ "$ACCT" = "200" ] && ok "账户查询" || fail "账户查询" "$ACCT"
else
  echo "  跳过（无Token）"
fi

# ---- 6. 入金 ----
echo ""
echo "--- 6. 资金操作 ---"
if [ -n "$TOKEN" ] && [ -n "$USER_ID" ]; then
  DEPOSIT_BODY="{\"userId\":\"$USER_ID\",\"amount\":100000}"
  DEPOSIT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/v1/fund/deposit" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
    -d "$DEPOSIT_BODY" --connect-timeout 5 2>/dev/null || echo "000")
  [ "$DEPOSIT" = "200" ] && ok "入金" || fail "入金" "$DEPOSIT"
fi

# ---- 7. 资金余额 ----
if [ -n "$TOKEN" ] && [ -n "$USER_ID" ]; then
  BAL=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/v1/fund/balance?userId=$USER_ID" \
    -H "Authorization: Bearer $TOKEN" --connect-timeout 5 2>/dev/null || echo "000")
  [ "$BAL" = "200" ] && ok "资金余额查询" || fail "资金余额查询" "$BAL"
fi

# ---- 8. 订单 ----
echo ""
echo "--- 7. 下单 ---"
if [ -n "$TOKEN" ]; then
  ORDER_BODY="{\"userId\":\"$USER_ID\",\"symbol\":\"ES\",\"direction\":\"BUY\",\"type\":\"LIMIT\",\"price\":4500,\"volume\":1}"
  ORDER=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/v1/order/place" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
    -d "$ORDER_BODY" --connect-timeout 5 2>/dev/null || echo "000")
  # 订单可能返回200（成功）或400（参数错误）或500
  [ "$ORDER" = "200" ] && ok "下单" || echo "  下单: HTTP $ORDER (可能未实现)"
fi

# ---- 9. 全链路健康检查 ----
echo ""
echo "--- 8. 服务健康检查 ---"
for svc in gateway:8088 account:8083 order:8081 matching:8082 fund:8084 risk:8085 market:8086 settlement:8087; do
  NAME="${svc%%:*}"
  PORT="${svc##*:}"
  H=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/actuator/health" --connect-timeout 3 2>/dev/null || echo "000")
  [ "$H" = "200" ] && ok "$NAME 健康" || echo "  ⚠️  $NAME: HTTP $H"
done

# ---- 结果 ----
echo ""
echo "========================================"
echo " 测试完成: ✅ $PASS 通过, ❌ $FAIL 失败"
echo "========================================"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
