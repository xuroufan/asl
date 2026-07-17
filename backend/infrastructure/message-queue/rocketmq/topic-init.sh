#!/bin/bash
# ============================================================
# RocketMQ Topic 初始化脚本
# 用法: ./topic-init.sh [NAMESRV_ADDR]
# 默认: localhost:9876
# ============================================================

set -euo pipefail

NAMESRV_ADDR="${1:-localhost:9876}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MQADMIN="${SCRIPT_DIR}/mqadmin"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
BOLD='\033[1m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}  期货交易平台 - RocketMQ Topic 初始化${NC}"
echo -e "${BOLD}  NameSrv: ${NAMESRV_ADDR}${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# ─── 检查 mqadmin 可用性 ───
if ! command -v mqadmin &>/dev/null; then
  echo -e "${YELLOW}⚠ mqadmin 未安装在 PATH 中${NC}"
  echo "  尝试从 RocketMQ 安装目录查找..."
  if [ -f /home/rocketmq/rocketmq-5.2.0/bin/mqadmin ]; then
    MQADMIN=/home/rocketmq/rocketmq-5.2.0/bin/mqadmin
    echo -e "${GREEN}  ✓ 找到: ${MQADMIN}${NC}"
  elif [ -f /opt/rocketmq/bin/mqadmin ]; then
    MQADMIN=/opt/rocketmq/bin/mqadmin
    echo -e "${GREEN}  ✓ 找到: ${MQADMIN}${NC}"
  else
    echo -e "${RED}✗ mqadmin 未找到${NC}"
    echo "  可以通过 Docker 执行:"
    echo "  docker exec futures-rocketmq-broker sh /home/rocketmq/rocketmq-5.2.0/bin/mqadmin updateTopic ..."
    exit 1
  fi
fi

# ─── Topic 定义 ───
# 格式: topicName:readQueueCount:writeQueueCount
declare -a TOPICS=(
  "futures-order-created:8:8"
  "futures-order-matched:8:8"
  "futures-order-cancelled:4:4"
  "futures-position-changed:4:4"
  "futures-risk-alert:2:2"
  "futures-market-tick:16:16"
  "futures-settlement-done:4:4"
)

echo -e "${YELLOW}[1/3] 创建 Topic 到集群 'futures-cluster'...${NC}"
echo ""

create_topic() {
  local topic="$1"
  local read_qs="$2"
  local write_qs="$3"
  
  echo -n "  创建 ${topic} (R:${read_qs}/W:${write_qs}) ... "
  
  # 使用 mqadmin 创建 Topic
  local output
  output=$("${MQADMIN}" updateTopic \
    -n "${NAMESRV_ADDR}" \
    -c futures-cluster \
    -t "${topic}" \
    -r "${read_qs}" \
    -w "${write_qs}" \
    -o 8 \
    2>&1 || true)
  
  if echo "$output" | grep -qi "success\|create topic"; then
    echo -e "${GREEN}✓${NC}"
  elif echo "$output" | grep -qi "already exists"; then
    echo -e "${YELLOW}⚠ 已存在${NC}"
  else
    echo -e "${RED}✗ ${output}${NC}"
  fi
}

for topic_def in "${TOPICS[@]}"; do
  IFS=':' read -r topic read_qs write_qs <<< "$topic_def"
  create_topic "$topic" "$read_qs" "$write_qs"
done

# ─── 创建消费者组 ───
echo ""
echo -e "${YELLOW}[2/3] 创建消费者组...${NC}"

declare -a CONSUMER_GROUPS=(
  "cg-order-matched"
  "cg-order-cancelled"
  "cg-position-changed"
  "cg-risk-alert"
  "cg-market-tick"
  "cg-settlement-done"
  "cg-order-created"
)

for group in "${CONSUMER_GROUPS[@]}"; do
  echo "  消费者组: ${group}"
  # mqadmin 中的消费者组会自动创建，无需手动执行
done

# ─── 验证 ───
echo ""
echo -e "${YELLOW}[3/3] 验证 Topic 列表...${NC}"
echo ""

# 通过 Docker 验证
if docker ps --format '{{.Names}}' | grep -q futures-rocketmq-ns; then
  docker exec futures-rocketmq-ns \
    sh /home/rocketmq/rocketmq-5.2.0/bin/mqadmin topicList \
    -n "${NAMESRV_ADDR}" 2>/dev/null | grep "futures-" || true
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Topic 初始化完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "  控制台: http://localhost:8089"
echo -e "  NameSrv: ${NAMESRV_ADDR}"
echo ""
