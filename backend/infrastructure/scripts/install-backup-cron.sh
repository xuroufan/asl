#!/bin/bash
# ============================================================
# 安装备份定时任务
# 使用方法: sudo ./install-backup-cron.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/data/backup}"

echo "安装备份定时任务..."
echo "备份目录: ${BACKUP_DIR}"
echo "脚本目录: ${SCRIPT_DIR}"

# 确保备份目录存在
mkdir -p "${BACKUP_DIR}/mysql"
mkdir -p "${BACKUP_DIR}/redis"

# 为脚本添加执行权限
chmod +x "${SCRIPT_DIR}/backup-mysql.sh"
chmod +x "${SCRIPT_DIR}/backup-redis.sh"

# 安装 MySQL 备份定时任务 (每天凌晨3:00)
(crontab -l 2>/dev/null || true; echo "# Futures Platform - MySQL 全量备份 (每天3:00)")
(crontab -l 2>/dev/null || true; echo "0 3 * * * ${SCRIPT_DIR}/backup-mysql.sh >> ${BACKUP_DIR}/mysql-backup.log 2>&1")

# 安装 Redis 备份定时任务 (每天凌晨4:00)
(crontab -l 2>/dev/null || true; echo "# Futures Platform - Redis 数据备份 (每天4:00)")
(crontab -l 2>/dev/null || true; echo "0 4 * * * ${SCRIPT_DIR}/backup-redis.sh >> ${BACKUP_DIR}/redis-backup.log 2>&1")

echo "✅ 备份定时任务已安装!"
echo ""
echo "当前定时任务列表:"
crontab -l 2>/dev/null | grep -E "backup|Futures" || echo "  (无备份任务)"
