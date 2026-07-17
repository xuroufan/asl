#!/bin/bash
# ============================================================
# MySQL 全量备份脚本
# 每日凌晨执行，保留30天
# ============================================================
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/data/backup/mysql}"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_USER="root"
MYSQL_PASS="futures123"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
S3_BUCKET="${S3_BUCKET:-}"

mkdir -p "${BACKUP_DIR}/${DATE}"

echo "[$(date)] 开始 MySQL 全量备份..."

# 全量备份 - 所有数据库
mysqldump \
  -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" \
  -u"${MYSQL_USER}" -p"${MYSQL_PASS}" \
  --all-databases \
  --gtid \
  --master-data=2 \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --hex-blob \
  --opt \
  --flush-logs \
  --delete-master-logs \
  | gzip > "${BACKUP_DIR}/${DATE}/futures_all_databases.sql.gz"

echo "[$(date)] 备份完成: ${BACKUP_DIR}/${DATE}/futures_all_databases.sql.gz"

# 检查备份完整性
if [ -f "${BACKUP_DIR}/${DATE}/futures_all_databases.sql.gz" ] && \
   [ $(stat -f%z "${BACKUP_DIR}/${DATE}/futures_all_databases.sql.gz" 2>/dev/null || stat -c%s "${BACKUP_DIR}/${DATE}/futures_all_databases.sql.gz") -gt 1000 ]; then
    echo "[$(date)] 备份完整性检查通过"
else
    echo "[ERROR] 备份文件异常: 不存在或小于1KB"
    exit 1
fi

# 清理过期备份
find "${BACKUP_DIR}" -maxdepth 2 -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null || true
echo "[$(date)] 已清理 ${RETENTION_DAYS} 天前的旧备份"

# 如有配置 S3 则同步到远端
if [ -n "${S3_BUCKET}" ]; then
    if command -v aws &>/dev/null; then
        aws s3 sync "${BACKUP_DIR}" "s3://${S3_BUCKET}/mysql-backups/" --storage-class STANDARD_IA
        echo "[$(date)] 备份已同步至 S3: s3://${S3_BUCKET}/mysql-backups/"
    fi
fi

echo "[$(date)] MySQL 备份流程完成"
