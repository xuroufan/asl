#!/bin/bash
# ============================================================
# Redis 数据备份脚本
# 手动触发 RDB 快照 + AOF 复制，保留 7 天本地副本
# ============================================================
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/data/backup/redis}"
RETENTION_DAYS=7
DATE=$(date +%Y%m%d_%H%M%S)
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASS="${REDIS_PASS:-futures123}"

mkdir -p "${BACKUP_DIR}/${DATE}"

echo "[$(date)] 开始 Redis 数据备份..."

# 1. 触发 RDB 快照
redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASS}" BGSAVE
echo "[$(date)] RDB BGSAVE 已触发，等待 30 秒..."
sleep 3

# 2. 获取 RDB 文件路径并拷贝
RDB_PATH=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASS}" CONFIG GET dir | tail -1)
RDB_FILE="${RDB_PATH}/dump.rdb"

if [ -f "${RDB_FILE}" ]; then
    cp "${RDB_FILE}" "${BACKUP_DIR}/${DATE}/dump.rdb"
    gzip "${BACKUP_DIR}/${DATE}/dump.rdb"
    echo "[$(date)] RDB 备份完成: ${BACKUP_DIR}/${DATE}/dump.rdb.gz"
else
    echo "[ERROR] RDB 文件不存在: ${RDB_FILE}"
fi

# 3. 拷贝 AOF 文件（如启用）
AOF_ENABLED=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASS}" CONFIG GET appendonly | tail -1)
AOF_FILE="${RDB_PATH}/appendonly.aof"
if [ "${AOF_ENABLED}" = "yes" ] && [ -f "${AOF_FILE}" ]; then
    cp "${AOF_FILE}" "${BACKUP_DIR}/${DATE}/appendonly.aof"
    gzip "${BACKUP_DIR}/${DATE}/appendonly.aof"
    echo "[$(date)] AOF 备份完成: ${BACKUP_DIR}/${DATE}/appendonly.aof.gz"
fi

# 4. 清理过期备份
find "${BACKUP_DIR}" -maxdepth 2 -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null || true
echo "[$(date)] 已清理 ${RETENTION_DAYS} 天前的旧备份"

echo "[$(date)] Redis 备份流程完成"
