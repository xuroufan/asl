#!/bin/bash
# ============================================================
# 数据库归档脚本
# 将 t_order / t_fund_flow / t_trade 的历史数据迁入归档库
# 使用: 在归档库所在的服务器上通过 cron 调度
# 依赖: mysql client (8.0+)
# ============================================================
# 用法:
#   ./db-archive.sh --mode dry-run       # 预览将要归档的行数
#   ./db-archive.sh --mode execute       # 实际执行归档
#   ./db-archive.sh --mode create-tables  # 创建归档表（首次运行）
#
# 配置推荐 cron (每日凌晨 3:00):
#   0 3 * * * /opt/scripts/db-archive.sh --mode execute >> /var/log/db-archive.log 2>&1
# ============================================================

set -euo pipefail

# ===================== 配置 =====================

# 在线库连接
ONLINE_HOST="${ARCHIVE_ONLINE_HOST:-trading-db-master.internal}"
ONLINE_PORT="${ARCHIVE_ONLINE_PORT:-3306}"
ONLINE_DB="${ARCHIVE_ONLINE_DB:-trading}"
ONLINE_USER="${ARCHIVE_ONLINE_USER:-archiver}"
ONLINE_PASS="${ARCHIVE_ONLINE_PASS:-}"

# 归档库连接
ARCHIVE_HOST="${ARCHIVE_HOST:-trading-db-archive.internal}"
ARCHIVE_PORT="${ARCHIVE_PORT:-3306}"
ARCHIVE_DB="${ARCHIVE_DB:-trading_archive}"
ARCHIVE_USER="${ARCHIVE_USER:-archiver}"
ARCHIVE_PASS="${ARCHIVE_PASS:-}"

# 归档时间窗口（默认迁移 3 个月前的已成交订单）
ARCHIVE_BEFORE_DAYS=${ARCHIVE_BEFORE_DAYS:-90}
BATCH_SIZE=${ARCHIVE_BATCH_SIZE:-500}
MODE="${1:-dry-run}"

# ===================== 函数 =====================

MYSQL_ONLINE="mysql -h $ONLINE_HOST -P $ONLINE_PORT -u $ONLINE_USER"
MYSQL_ARCHIVE="mysql -h $ARCHIVE_HOST -P $ARCHIVE_PORT -u $ARCHIVE_USER"

if [ -n "$ONLINE_PASS" ]; then MYSQL_ONLINE="$MYSQL_ONLINE -p$ONLINE_PASS"; fi
if [ -n "$ARCHIVE_PASS" ]; then MYSQL_ARCHIVE="$MYSQL_ARCHIVE -p$ARCHIVE_PASS"; fi

MYSQL_ONLINE="$MYSQL_ONLINE $ONLINE_DB"
MYSQL_ARCHIVE="$MYSQL_ARCHIVE $ARCHIVE_DB"

now_ts() {
    date +%s000  # 毫秒
}

log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $*"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $*" >&2
}

# ===================== 创建归档表 =====================

create_archive_tables() {
    log_info "创建归档表 (如果不存在)..."

    # 在线库的 CREATE TABLE LIKE，归档库独立执行
    $MYSQL_ARCHIVE -e "
        CREATE TABLE IF NOT EXISTS t_order_archive (
            id BIGINT NOT NULL,
            order_id VARCHAR(64) NOT NULL,
            user_id BIGINT NOT NULL,
            symbol VARCHAR(32) NOT NULL,
            type VARCHAR(16) NOT NULL,
            side VARCHAR(8) NOT NULL,
            status VARCHAR(16) NOT NULL,
            price DECIMAL(24,8) DEFAULT NULL,
            stop_price DECIMAL(24,8) DEFAULT NULL,
            quantity DECIMAL(24,8) NOT NULL,
            filled_qty DECIMAL(24,8) NOT NULL DEFAULT 0,
            avg_price DECIMAL(24,8) DEFAULT NULL,
            total_amount DECIMAL(24,8) NOT NULL,
            fee DECIMAL(24,8) NOT NULL DEFAULT 0,
            created_at BIGINT NOT NULL,
            updated_at BIGINT NOT NULL,
            archived_at BIGINT NOT NULL,
            PRIMARY KEY (id),
            INDEX idx_user_id (user_id),
            INDEX idx_created_at (created_at),
            INDEX idx_archived_at (archived_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单归档表';

        CREATE TABLE IF NOT EXISTS t_fund_flow_archive LIKE t_fund_flow;
        ALTER TABLE t_fund_flow_archive ADD COLUMN archived_at BIGINT NOT NULL AFTER created_at;
        ALTER TABLE t_fund_flow_archive ADD INDEX idx_archived_at (archived_at);

        CREATE TABLE IF NOT EXISTS t_trade_archive LIKE t_trade;
        ALTER TABLE t_trade_archive ADD COLUMN archived_at BIGINT NOT NULL AFTER executed_at;
        ALTER TABLE t_trade_archive ADD INDEX idx_archived_at (archived_at);
    "
    log_info "归档表创建完成"
}

# ===================== 预览归档行数 =====================

dry_run() {
    local before_ts=$(($(date +%s000) - ARCHIVE_BEFORE_DAYS * 86400 * 1000))

    echo "========================================="
    echo " 归档预览（$ARCHIVE_BEFORE_DAYS 天前的数据）"
    echo "========================================="

    for table in "t_order" "t_fund_flow" "t_trade"; do
        local count=$($MYSQL_ONLINE -N -e "
            SELECT COUNT(*) FROM $table
            WHERE (CASE WHEN '$table' = 't_trade' THEN executed_at ELSE created_at END) < $before_ts
            AND (CASE WHEN '$table' = 't_order' THEN status = 'FILLED' ELSE 1 END)
        " 2>/dev/null || echo 0)
        echo "  $table: $count 行"
    done

    echo ""
    echo "运行以下命令执行归档:"
    echo "  $0 --mode execute"
}

# ===================== 执行归档 =====================

archive_table() {
    local table=$1
    local archive_table="${table}_archive"
    local time_column="${2:-created_at}"
    local status_filter="${3:-}"

    local before_ts=$(($(date +%s000) - ARCHIVE_BEFORE_DAYS * 86400 * 1000))
    local total=0

    log_info "开始归档 $table -> $archive_table (时间列: $time_column, 截止: $(date -d @$((before_ts / 1000)) '+%Y-%m-%d'))"

    while true; do
        # 读取一批数据到临时文件
        $MYSQL_ONLINE -N -e "
            SELECT id FROM $table
            WHERE $time_column < $before_ts
            $status_filter
            LIMIT $BATCH_SIZE
        " > /tmp/archive_batch_ids_$$.txt

        local batch_count=$(wc -l < /tmp/archive_batch_ids_$$.txt | tr -d ' ')
        [ "$batch_count" -eq 0 ] && break

        # 事务内执行: INSERT + DELETE
        $MYSQL_ONLINE -e "START TRANSACTION;
            INSERT IGNORE INTO $ARCHIVE_DB.$archive_table
            SELECT t.*, UNIX_TIMESTAMP() * 1000
            FROM $table t
            WHERE t.id IN ($(paste -sd, /tmp/archive_batch_ids_$$.txt));

            DELETE FROM $table
            WHERE id IN ($(paste -sd, /tmp/archive_batch_ids_$$.txt));
        COMMIT;"

        if [ $? -eq 0 ]; then
            total=$((total + batch_count))
            log_info "  归档 $batch_count 条 (累计 $total)"
        else
            log_error "  归档失败: $table, batch_size=$batch_count"
            return 1
        fi

        # 防止数据库压力过大，每批间等待 200ms
        sleep 0.2
    done

    log_info "归档完成: $table -> $archive_table, 共 $total 条"
}

execute() {
    local before_ts=$(($(date +%s000) - ARCHIVE_BEFORE_DAYS * 86400 * 1000))

    log_info "========================================="
    log_info "开始数据归档 (阈值: $ARCHIVE_BEFORE_DAYS 天)"
    log_info "在线库: $ONLINE_HOST:$ONLINE_PORT/$ONLINE_DB"
    log_info "归档库: $ARCHIVE_HOST:$ARCHIVE_PORT/$ARCHIVE_DB"
    log_info "========================================="

    # 1. 归档已成交订单（online查询时用 created_at）
    archive_table "t_order" "created_at" "AND status = 'FILLED'"

    # 2. 归档资金流水（所有状态，仅按时间）
    archive_table "t_fund_flow" "created_at" ""

    # 3. 归档成交记录
    archive_table "t_trade" "executed_at" ""

    log_info "========================================="
    log_info "全部归档完成!"
    log_info "========================================="

    # 清理临时文件
    rm -f /tmp/archive_batch_ids_$$.txt
}

# ===================== 主入口 =====================

case "$MODE" in
    dry-run)
        dry_run
        ;;
    execute)
        execute
        ;;
    create-tables)
        create_archive_tables
        ;;
    *)
        echo "用法: $0 [--mode dry-run|execute|create-tables]"
        echo ""
        echo "  dry-run        预览将归档的行数（不执行）"
        echo "  execute        实际执行归档"
        echo "  create-tables  创建归档表（首次运行）"
        exit 1
        ;;
esac
