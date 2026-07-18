#!/bin/bash
# ================================================
# ASL 期货平台 — 管理脚本 v4.0
# 用法: ./start.sh <command> [service]
#
# 命令:
#   start      启动所有服务（默认）
#   stop       停止所有服务
#   restart    重启所有服务
#   status     查看所有服务状态
#   logs       查看服务日志
#   help       显示帮助
# ================================================

BASE="$(cd "$(dirname "$0")" && pwd)"
LOG="$BASE/logs"
JAVA21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/java"

# ---- 参数 ----
NACOS_OK="--spring.cloud.nacos.discovery.enabled=false --spring.cloud.nacos.config.enabled=false --spring.cloud.nacos.server-addr=127.0.0.1:1"
JVM_OK="-Dnacos.remote.client.grpc.reconnect.max.retry.count=0"
BASE_FLAGS="$NACOS_OK --seata.enabled=false --spring.config.additional-location=file:$BASE/config/health-fix.yml"
PUSH_XCL="--spring.autoconfigure.exclude=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
FUND_XCL="--spring.autoconfigure.exclude=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"

# ---- 辅助函数 ----
die() { echo "❌ $1"; exit 1; }
info() { echo "  $1"; }

# ---- 启动单个服务 ----
start_svc() {
  local name="$1" port="$2" jar_dir="$3" extra="$4"
  if tmux has-session -t "svc-$name" 2>/dev/null; then
    info "  $name($port) 已在运行"
    return
  fi
  info "  启动 $name($port)..."
  tmux new-session -d -s "svc-$name" "cd $BASE && $JAVA21 -Xmx256m $JVM_OK -jar $jar_dir/target/futures-$name-1.0.0-SNAPSHOT.jar $BASE_FLAGS $extra 2>&1 | tee $LOG/$name.log"
  sleep 2
}

# ---- 启动全部 ----
cmd_start() {
  # 等待 Docker
  echo "等待 Docker..."
  for i in $(seq 1 30); do
    if docker ps >/dev/null 2>&1; then
      info "  Docker 就绪 (${i}s)"
      break
    fi
    if [ "$i" -eq 30 ]; then open -a Docker; fi
    sleep 2
  done

  # 启动 MySQL/Redis
  if docker ps >/dev/null 2>&1; then
    cd "$BASE/backend"
    docker compose up -d mysql-master redis-master 2>/dev/null
    echo "  等待 MySQL/Redis..."
    for i in $(seq 1 15); do
      mysql_ok=$(docker ps --filter "name=futures-mysql-master" --filter "health=healthy" -q 2>/dev/null)
      redis_ok=$(docker ps --filter "name=futures-redis-master" --filter "health=healthy" -q 2>/dev/null)
      [ -n "$mysql_ok" ] && [ -n "$redis_ok" ] && { info "  就绪"; break; }
      sleep 2
    done
  fi

  # 初始化数据库
  if docker ps | grep -q futures-mysql-master; then
    docker exec -i futures-mysql-master mysql -u root -pfutures123 < "$BASE/backend/infrastructure/scripts/init-schema.sql" 2>/dev/null
    info "  数据库表已同步"
  fi

  echo ""
  echo "=== 启动服务 ==="
  start_svc order      8081 backend/futures-order
  start_svc matching   8082 backend/futures-matching
  start_svc account    8083 backend/futures-account
  start_svc fund       8084 backend/futures-fund    "$FUND_XCL"
  start_svc risk       8085 backend/futures-risk
  start_svc market     8086 backend/futures-market
  start_svc settlement 8087 backend/futures-settlement
  start_svc push       8093 backend/futures-push    "$PUSH_XCL"
  start_svc admin      8099 backend/futures-admin

  if ! tmux has-session -t gateway 2>/dev/null; then
    info "  启动 gateway(8088)..."
    tmux new-session -d -s gateway "cd $BASE && $JAVA21 -Xmx96m -jar backend/futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar 2>&1 | tee $LOG/gateway.log"
  fi

  echo ""
  echo "=== 等待就绪 ==="
  sleep 20

  echo ""
  echo "=== 状态 ==="
  cmd_status
}

# ---- 停止全部 ----
cmd_stop() {
  echo "=== 停止全部 ==="
  for tmux_sess in svc-order svc-matching svc-account svc-fund svc-risk svc-market svc-settlement svc-push svc-admin gateway admin-ui web watchdog-loop; do
    if tmux has-session -t "$tmux_sess" 2>/dev/null; then
      tmux kill-session -t "$tmux_sess" 2>/dev/null
      info "  tmux $tmux_sess 已停止"
    fi
  done
  # 确保 Java 进程都停掉
  for port in 8081 8082 8083 8084 8085 8086 8087 8093 8099 8088; do
    kill -9 $(lsof -ti:$port 2>/dev/null) 2>/dev/null
  done
  info "  所有服务已停止"
}

# ---- 状态查看 ----
cmd_status() {
  echo "=== 服务状态 ==="
  echo ""
  for spec in "svc-order:8081:订单服务" "svc-matching:8082:撮合引擎" "svc-account:8083:账户服务" "svc-fund:8084:资金服务" "svc-risk:8085:风控引擎" "svc-market:8086:行情服务" "svc-settlement:8087:结算服务" "svc-push:8093:推送服务" "svc-admin:8099:管理后台" "gateway:8088:API网关"; do
    IFS=':' read -r tmux_name port label <<< "$spec"
    session_ok=$(tmux has-session -t "$tmux_name" 2>/dev/null && echo "✅" || echo "❌")
    health=$(curl -s -o /dev/null -w "%{http_code}" --max-time 1 http://localhost:$port/actuator/health 2>/dev/null || echo "000")
    case "$health" in
      200) health_icon="✅" ;;
      000) health_icon="❌" ;;
      *) health_icon="⚠️" ;;
    esac
    printf "  %-12s %s tmux=%s health=%s\n" "$label" "$health_icon" "$session_ok" "$health"
  done

  echo ""
  echo "=== Docker ==="
  if docker ps >/dev/null 2>&1; then
    docker ps --format "  {{.Names}}: {{.Status}}" 2>/dev/null
  else
    echo "  ❌ Docker 未运行"
  fi

  echo ""
  echo "=== 前端 ==="
  for spec2 in "localhost:5173:Web" "localhost:8090:Admin"; do
    IFS=':' read -r host port label <<< "$spec2"
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 1 "http://$host:$port" 2>/dev/null || echo "000")
    [ "$code" = "200" ] && icon="✅" || icon="❌"
    echo "  $icon $label: HTTP $code"
  done
}

# ---- 日志查看 ----
cmd_logs() {
  local svc_name="$1"
  [ -z "$svc_name" ] && die "用法: ./start.sh logs <service>\n  服务: order matching account fund risk market settlement push admin gateway"
  local logfile="$LOG/$svc_name.log"
  [ ! -f "$logfile" ] && die "日志文件不存在: $logfile"
  tail -f "$logfile"
}

# ---- 主入口 ----
CMD="${1:-help}"
case "$CMD" in
  start|up)
    cmd_start
    ;;
  stop|down)
    cmd_stop
    ;;
  restart)
    cmd_stop
    echo ""
    cmd_start
    ;;
  status|ps)
    cmd_status
    ;;
  logs)
    cmd_logs "$2"
    ;;
  help|--help|-h|"")
    echo "用法: ./start.sh <命令> [参数]"
    echo ""
    echo "命令:"
    echo "  start      启动所有服务（默认）"
    echo "  stop       停止所有服务"
    echo "  restart    重启所有服务"
    echo "  status     查看所有服务状态"
    echo "  logs NAME  查看服务日志 (order/matching/account/...)"
    echo ""
    echo "示例:"
    echo "  ./start.sh status        # 查看所有服务状态"
    echo "  ./start.sh restart       # 重启全部"
    echo "  ./start.sh logs matching  # 查看撮合引擎日志"
    ;;
  *)
    die "未知命令: $CMD\n  用法: ./start.sh help"
    ;;
esac
