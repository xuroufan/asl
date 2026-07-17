#!/bin/bash
# ============================================================
# Kafka Topic 初始化脚本
# 用法: ./topic-init.sh [BOOTSTRAP_SERVER]
# 默认: localhost:9092
# ============================================================

set -euo pipefail

BOOTSTRAP_SERVER="${1:-localhost:9092}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
BOLD='\033[1m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}  期货交易平台 - Kafka Topic 初始化${NC}"
echo -e "${BOLD}  Bootstrap: ${BOOTSTRAP_SERVER}${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# ─── 检查 kafka-topics.sh 可用性 ───
KAFKA_TOPICS=""
if command -v kafka-topics &>/dev/null; then
  KAFKA_TOPICS="kafka-topics"
elif docker ps --format '{{.Names}}' | grep -q futures-kafka; then
  KAFKA_TOPICS="docker exec futures-kafka kafka-topics"
  echo -e "${YELLOW}  使用 Docker 容器执行${NC}"
else
  echo -e "${RED}✗ kafka-topics 未找到，请将 Kafka bin 目录加入 PATH${NC}"
  echo "  或通过 Docker 执行:"
  echo "  docker exec futures-kafka kafka-topics --bootstrap-server localhost:9092 ..."
  exit 1
fi

# ─── Topic 定义 ───
declare -a TOPICS=(
  "futures-market-tick:16:1:1440min"       # 高吞吐行情流
  "futures-market-kline:8:1:4320min"        # K线数据
  "futures-market-depth:8:1:1440min"        # 盘口深度数据
  "futures-trade-audit:4:1:10080min"        # 交易审计日志(保留7天)
  "futures-system-log:4:1:4320min"          # 系统日志
)

echo -e "${YELLOW}[1/2] 创建 Topic...${NC}"
echo ""

for topic_def in "${TOPICS[@]}"; do
  IFS=':' read -r topic partitions replication retention <<< "$topic_def"
  
  echo -n "  创建 ${topic} (P:${partitions}/R:${replication}) ... "
  
  $KAFKA_TOPICS --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor "${replication}" \
    --config retention.ms="${retention}" \
    --if-not-exists \
    2>&1 | head -1 || echo -e "${RED}✗${NC}"
done

# ─── 验证 ───
echo ""
echo -e "${YELLOW}[2/2] 验证 Topic 列表...${NC}"
echo ""

$KAFKA_TOPICS --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --list 2>/dev/null | grep "futures-" || echo -e "${YELLOW}  未找到 futures- 开头的 Topic${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Topic 初始化完成！${NC}"
echo -e "${GREEN}========================================${NC}"
