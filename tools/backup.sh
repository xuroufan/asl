#!/bin/bash
# ================================================
# ASL 期货平台 — 数据库备份脚本
# 备份: MySQL (所有 futures_* 数据库) + Redis (RDB)
# 保留: 最近 7 天
# 用法: ./tools/backup.sh
# ================================================

BASE="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="$BASE/backups"
DATE=$(date '+%Y%m%d_%H%M%S')
LOG="$BACKUP_DIR/backup.log"

mkdir -p "$BACKUP_DIR"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"; }

log "=== 开始备份 ==="

# ---- 1. MySQL 备份 ----
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'futures-mysql-master'; then
  DB_FILE="$BACKUP_DIR/mysql_futures_$DATE.sql.gz"
  log "备份 MySQL..."
  docker exec futures-mysql-master mysqldump -u root -pfutures123 \
    --databases futures_order futures_account futures_fund futures_risk \
                futures_market futures_settlement futures_push futures_admin \
    --single-transaction --routines --events 2>/dev/null | gzip > "$DB_FILE"
  if [ -f "$DB_FILE" ] && [ -s "$DB_FILE" ]; then
    log "  ✅ MySQL: $(du -h "$DB_FILE" | cut -f1)"
  else
    log "  ❌ MySQL 备份失败"
    rm -f "$DB_FILE"
  fi
else
  log "  ⚠️ MySQL 未运行"
fi

# ---- 2. Redis 备份 ----
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'futures-redis-master'; then
  REDIS_FILE="$BACKUP_DIR/redis_futures_$DATE.rdb.gz"
  log "备份 Redis..."
  docker exec futures-redis-master redis-cli -a futures123 SAVE 2>/dev/null
  docker cp futures-redis-master:/data/dump.rdb - 2>/dev/null | gzip > "$REDIS_FILE"
  if [ -f "$REDIS_FILE" ] && [ -s "$REDIS_FILE" ]; then
    log "  ✅ Redis: $(du -h "$REDIS_FILE" | cut -f1)"
  else
    log "  ❌ Redis 备份失败"
    rm -f "$REDIS_FILE"
  fi
else
  log "  ⚠️ Redis 未运行"
fi

# ---- 3. 清理旧备份（7 天） ----
log "清理 7 天前的备份..."
CLEANED=0
for f in "$BACKUP_DIR"/mysql_futures_*.sql.gz "$BACKUP_DIR"/redis_futures_*.rdb.gz; do
  [ -f "$f" ] || continue
  age=$(( ($(date +%s) - $(stat -f "%m" "$f")) / 86400 ))
  if [ "$age" -gt 7 ]; then
    rm -f "$f"
    log "  🗑️ $(basename "$f") (${age}天)"
    CLEANED=$((CLEANED+1))
  fi
done
[ "$CLEANED" -eq 0 ] && log "  无过期备份"

# ---- 4. 统计 ----
TOTAL=$(ls -1 "$BACKUP_DIR"/*.sql.gz "$BACKUP_DIR"/*.rdb.gz 2>/dev/null | wc -l)
log "备份总数: $TOTAL | 总大小: $(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1)"
log "=== 完成 ==="
