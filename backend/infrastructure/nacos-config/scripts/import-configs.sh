#!/bin/bash
# ============================================================
# Nacos 配置批量导入脚本
# 将本地 YAML 配置文件导入 Nacos 配置中心
#
# 使用方式:
#   chmod +x import-configs.sh
#   ./import-configs.sh                    # 导入所有 dev 配置
#   ./import-configs.sh prod               # 导入所有 prod 配置
#   ./import-configs.sh dev gateway        # 只导入 gateway 的 dev 配置
# ============================================================

set -euo pipefail

# ─── 配置 ───
NACOS_HOST="${NACOS_HOST:-localhost}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_USER="${NACOS_USER:-nacos}"
NACOS_PASS="${NACOS_PASS:-nacos}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-}"
PROFILE="${1:-dev}"          # 第一个参数: 环境 (dev/prod)
SERVICE_FILTER="${2:-}"      # 第二个参数: 指定服务 (可选)

# ─── 目录 ───
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")"
GLOBAL_DIR="$CONFIG_DIR/global"
SERVICE_DIR="$CONFIG_DIR/services"

# ─── 颜色 ───
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# ─── Nacos 登录获取 Token ───
echo -e "${YELLOW}[1/4] 登录 Nacos...${NC}"
LOGIN_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/auth/login"
LOGIN_RESP=$(curl -s -X POST "$LOGIN_URL" \
  -d "username=${NACOS_USER}&password=${NACOS_PASS}")

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null || echo "")

if [ -z "$ACCESS_TOKEN" ]; then
  echo -e "${RED}✗ Nacos 登录失败${NC}"
  echo "  响应: $LOGIN_RESP"
  exit 1
fi
echo -e "${GREEN}✓ Nacos 登录成功${NC}"

# ─── 导入配置函数 ───
import_config() {
  local data_id="$1"
  local group="$2"
  local file_path="$3"

  if [ ! -f "$file_path" ]; then
    echo -e "${YELLOW}  跳过 ${data_id} (文件不存在)${NC}"
    return
  fi

  # 读取文件内容
  local content
  content=$(cat "$file_path")

  # 调用 Nacos API 发布配置
  PUBLISH_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs"
  RESP=$(curl -s -X POST "$PUBLISH_URL" \
    -d "dataId=${data_id}" \
    -d "group=${group}" \
    -d "namespaceId=${NACOS_NAMESPACE}" \
    -d "content=${content}" \
    -d "type=yaml" \
    -d "accessToken=${ACCESS_TOKEN}" \
    -d "desc=Auto-imported from local configs")

  if [ "$RESP" = "true" ]; then
    echo -e "${GREEN}  ✓ ${data_id} (${group})${NC}"
  else
    echo -e "${RED}  ✗ ${data_id}: ${RESP}${NC}"
  fi
}

# ─── 导入全局配置 ───
echo ""
echo -e "${YELLOW}[2/4] 导入全局配置...${NC}"

import_config "futures-shared-${PROFILE}.yaml" "GLOBAL_GROUP" \
  "${GLOBAL_DIR}/futures-shared-${PROFILE}.yaml"

# ─── 导入服务配置 ───
echo ""
echo -e "${YELLOW}[3/4] 导入服务配置...${NC}"

if [ -n "$SERVICE_FILTER" ]; then
  # 只导入指定服务的配置
  for f in "${SERVICE_DIR}/futures-${SERVICE_FILTER}-${PROFILE}.yaml"; do
    if [ -f "$f" ]; then
      svc_name=$(basename "$f" | sed "s/-${PROFILE}.yaml//")
      data_id=$(basename "$f")
      echo "  服务: ${svc_name}"
      import_config "$data_id" "DEFAULT_GROUP" "$f"
    else
      echo -e "${YELLOW}  未找到 ${SERVICE_FILTER} 的 ${PROFILE} 配置${NC}"
    fi
  done
else
  # 导入所有服务配置
  for f in "${SERVICE_DIR}/futures-*-${PROFILE}.yaml"; do
    [ -f "$f" ] || continue
    svc_name=$(basename "$f" | sed "s/-${PROFILE}.yaml//")
    data_id=$(basename "$f")
    import_config "$data_id" "DEFAULT_GROUP" "$f"
  done
fi

# ─── 验证导入结果 ───
echo ""
echo -e "${YELLOW}[4/4] 验证配置导入...${NC}"

echo ""
echo "  Nacos 控制台: http://${NACOS_HOST}:${NACOS_PORT}/nacos"
echo "  账号: ${NACOS_USER} / ${NACOS_PASS}"
echo "  命名空间: ${NACOS_NAMESPACE:-public}"
echo "  环境: ${PROFILE}"
echo ""

# 列出已导入的配置
LIST_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs"
curl -s "$LIST_URL" \
  -d "pageNo=1" \
  -d "pageSize=20" \
  -d "namespaceId=${NACOS_NAMESPACE}" \
  -d "accessToken=${ACCESS_TOKEN}" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    total = data.get('totalCount', 0)
    print(f'  Nacos 中已存在 {total} 条配置')
    for item in data.get('pageItems', []):
        print(f'    - {item[\"dataId\"]} ({item.get(\"group\", \"\")})')
except:
    print('  无法解析响应')
" 2>/dev/null || true

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  配置导入完成!${NC}"
echo -e "${GREEN}========================================${NC}"
