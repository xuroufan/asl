#!/bin/bash
# =========================================================
# 蓝绿部署脚本 — Docker Compose 版本
# =========================================================
# 策略:
#   1. 维护两套 Compose 项目: futures-blue / futures-green
#   2. 始终部署到非活跃一侧
#   3. 健康检查通过后切换 Nginx 上游
#   4. 保留旧版本 5 分钟后清理
# =========================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_DIR="${SCRIPT_DIR}/../docker"
NGINX_CONF="${COMPOSE_DIR}/nginx-bluegreen.conf"
PROJECT_NAME="futures"

# 当前活跃侧
ACTIVE_SIDE=$(curl -s http://localhost:8088/actuator/health 2>/dev/null | grep -c '"status":"UP"' > /dev/null && cat /tmp/active_side 2>/dev/null || echo "blue")
INACTIVE_SIDE=$([ "$ACTIVE_SIDE" = "blue" ] && echo "green" || echo "blue")

echo "=== 蓝绿部署 ==="
echo "活跃: ${ACTIVE_SIDE}"
echo "待部署: ${INACTIVE_SIDE}"

# 1. 构建新版本（可跳过）
# cd /workspace && mvn clean package -DskipTests

# 2. 部署到非活跃侧
echo "→ 部署到 ${INACTIVE_SIDE}..."
INACTIVE_COMPOSE="${COMPOSE_DIR}/docker-compose.${INACTIVE_SIDE}.yml"

if [ ! -f "$INACTIVE_COMPOSE" ]; then
  echo "⚠  ${INACTIVE_COMPOSE} 不存在，使用主 compose 文件"
  INACTIVE_COMPOSE="${COMPOSE_DIR}/docker-compose.yml"
fi

docker compose -p "${PROJECT_NAME}-${INACTIVE_SIDE}" -f "$INACTIVE_COMPOSE" up -d --wait --wait-timeout 120 2>&1

# 3. 健康检查
echo "→ 健康检查..."
sleep 10

HEALTH_PASS=true
for PORT in 8088 8089 8099; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" = "200" ] || [ "$STATUS" = "403" ]; then
    echo "  ✅ :${PORT} → ${STATUS}"
  else
    echo "  ❌ :${PORT} → ${STATUS}"
    HEALTH_PASS=false
  fi
done

if [ "$HEALTH_PASS" = "false" ]; then
  echo "❌ 健康检查失败，回滚部署..."
  docker compose -p "${PROJECT_NAME}-${INACTIVE_SIDE}" -f "$INACTIVE_COMPOSE" down
  exit 1
fi

# 4. 切换流量
echo "→ 切换流量到 ${INACTIVE_SIDE}..."
echo "${INACTIVE_SIDE}" > /tmp/active_side

# 如果使用 Nginx，重新加载配置
if [ -f "$NGINX_CONF" ]; then
  # 替换 upstream 中的 server 指向
  sed -i '' "s/upstream-${ACTIVE_SIDE}/upstream-${INACTIVE_SIDE}/g" "$NGINX_CONF"
  nginx -s reload 2>/dev/null || echo "  ⚠ Nginx 重载失败（Nginx 可能未运行）"
fi

echo ""
echo "=== 部署完成 ✅ ==="
echo "新活跃侧: ${INACTIVE_SIDE}"

# 5. 清理旧版本（5分钟后）
echo "→ 旧版本 (${ACTIVE_SIDE}) 将在 5 分钟后自动清理..."
(
  sleep 300
  docker compose -p "${PROJECT_NAME}-${ACTIVE_SIDE}" -f "${COMPOSE_DIR}/docker-compose.${ACTIVE_SIDE}.yml" down 2>/dev/null || true
  echo "→ 旧版本 (${ACTIVE_SIDE}) 已清理"
) &
