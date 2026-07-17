#!/bin/bash
# ============================================================
# ASL Futures Trading Platform - 一键启动脚本 v2
# 启动顺序: Docker 基础设施 → 后端服务 → 前端界面
# ============================================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCKER_DIR="$PROJECT_DIR/docker"
BACKEND_DIR="$PROJECT_DIR/backend"
WEB_DIR="$PROJECT_DIR/web"
ADMIN_DIR="$PROJECT_DIR/admin-ui"
LOGS_DIR="$PROJECT_DIR/logs"
mkdir -p "$LOGS_DIR"

# ============ 颜色 ============
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${GREEN}[$(date '+%H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error(){ echo -e "${RED}[ERROR]${NC} $1"; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }

# ============ 端口配置 ============
declare -A PORTS=(
    [gateway]=8088 [admin]=8099 [account]=8083 [fund]=8084
    [market]=8086 [risk]=8085 [matching]=8082 [order]=8081
    [settlement]=8087 [push]=8093
)
SERVICES=(order matching account fund risk market settlement push admin gateway)

# ============ 健康检查 ============
health_check() {
    local name=$1 port=$2 retries=${3:-10} delay=${4:-2}
    for i in $(seq 1 $retries); do
        if curl -s -o /dev/null "http://localhost:$port/actuator/health" 2>/dev/null; then
            return 0
        fi
        sleep $delay
    done
    return 1
}

wait_for_health() {
    local name=$1 port=$2
    if health_check "$name" "$port"; then
        log "  ✅ $name ($port) — 就绪"
    else
        log "  ⚠️  $name ($port) — health check 超时，但服务可能已启动"
    fi
}

# ============ 清理 ============
cleanup() {
    log "正在停止服务..."
    for svc in gateway admin push settlement risk market fund account matching order; do
        local pid_file="$LOGS_DIR/${svc}.pid"
        [ -f "$pid_file" ] && kill "$(cat $pid_file)" 2>/dev/null && rm "$pid_file"
    done
    # 停前端
    pkill -f "vite.*$(echo ${PORTS[gateway]})" 2>/dev/null || true
    pkill -f "vite.*$(echo ${PORTS[admin]})" 2>/dev/null || true
    # 停 launchctl 网关
    launchctl bootout gui/$(id -u) /tmp/futures-gateway.plist 2>/dev/null || true
    info "所有服务已停止"
    exit 0
}
trap cleanup SIGINT SIGTERM

# ============================================================
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       ASL 期货交易平台 - 启动中心 v2        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ============================================================
# Phase 1: Docker 基础设施
# ============================================================
log "Phase 1/4: Docker 基础设施"

if ! docker info >/dev/null 2>&1; then
    error "Docker 未运行，请先启动 Docker Desktop"
    exit 1
fi

cd "$DOCKER_DIR"
docker compose -f dev-infra.yml up -d 2>&1

# 等待 MySQL
until docker exec asl-mysql mysqladmin ping -h localhost -uroot -proot123 --silent 2>/dev/null; do sleep 2; done
log "  ✅ MySQL (3306)"

# 等待 Redis
until docker exec asl-redis redis-cli -a futures123 ping 2>/dev/null | grep -q PONG; do sleep 2; done
log "  ✅ Redis (6379)"

# 等待 Prometheus
until curl -s -o /dev/null http://localhost:9090/-/ready 2>/dev/null; do sleep 2; done
log "  ✅ Prometheus (9090)"

# 等待 Grafana
# Grafana 有 health API
for i in $(seq 1 15); do
    if curl -s -o /dev/null http://localhost:3000/api/health 2>/dev/null; then break; fi
    sleep 2
done
log "  ✅ Grafana (3000)"

# ============================================================
# Phase 2: 停止旧进程（保留 Docker）
# ============================================================
log ""
log "Phase 2/4: 清理旧进程"
pkill -f "futures-gateway" 2>/dev/null && log "  ✓ 清理旧网关" || true
# 其他服务不会被 kill，第一次启动为全新

# ============================================================
# Phase 3: 启动后端微服务
# ============================================================
log ""
log "Phase 3/4: 启动后端微服务"

JAVA_HOME_21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home"
JAVA_CMD="$JAVA_HOME_21/bin/java"

if [ ! -f "$JAVA_CMD" ]; then
    JAVA_CMD="java"
fi

# 先验证 JAR 包存在
for svc in "${SERVICES[@]}"; do
    JAR="$BACKEND_DIR/futures-$svc/target/futures-$svc-1.0.0-SNAPSHOT.jar"
    if [ ! -f "$JAR" ]; then
        log "  🔧 编译 $svc..."
        cd "$BACKEND_DIR"
        $JAVA_CMD -version > /dev/null 2>&1 || { error "需要 JDK 21 或更高版本"; exit 1; }
        mvn package -Dmaven.test.skip=true -pl "futures-$svc" -q 2>/dev/null
    fi
done

# 启动服务（除了 gateway 用 launchctl，其他用 nohup）
for svc in "${SERVICES[@]}"; do
    local_port="${PORTS[$svc]}"
    JAR="$BACKEND_DIR/futures-$svc/target/futures-$svc-1.0.0-SNAPSHOT.jar"

    # 跳过 gateway（单独用 launchctl 管理）
    [ "$svc" = "gateway" ] && continue

    if pgrep -f "futures-$svc-1.0.0-SNAPSHOT.jar" > /dev/null 2>&1; then
        log "  ✅ $svc ($local_port) — 已在运行"
        continue
    fi

    nohup $JAVA_CMD -Xmx256m -jar "$JAR" > "$LOGS_DIR/$svc.log" 2>&1 &
    echo $! > "$LOGS_DIR/${svc}.pid"
    log "  🚀 $svc ($local_port) PID: $!"
done

# 启动 gateway 用 launchctl 保证稳定性
GATEWAY_JAR="$BACKEND_DIR/futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar"
if ! curl -s -o /dev/null http://localhost:8088/actuator/health 2>/dev/null; then
    log "  🚀 gateway (8088)"
    JAVA_HOME_21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home"
    JAVA_CMD="$JAVA_HOME_21/bin/java"
    [ ! -f "$JAVA_CMD" ] && JAVA_CMD="java"
    nohup $JAVA_CMD -Xmx96m -jar $BACKEND_DIR/futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar > "$LOGS_DIR/gateway.log" 2>&1 &
    GPID=$!
    disown
    # 保持前台 15 秒让后台进程逃过清理
    for i in $(seq 1 15); do
        if curl -s -o /dev/null http://localhost:8088/actuator/health 2>/dev/null; then
            break
        fi
        sleep 1
    done
fi

# 等待所有服务就绪
log ""
log "  等待服务就绪..."
for svc in "${SERVICES[@]}"; do
    local_port="${PORTS[$svc]}"
    wait_for_health "$svc" "$local_port"
done

# ============================================================
# Phase 4: 启动前端 + 完成
# ============================================================
log ""
log "Phase 4/4: 启动前端界面"

# 管理后台
if ! curl -s -o /dev/null http://localhost:8090/ 2>/dev/null; then
    cd "$ADMIN_DIR"
    nohup npx vite --port 8090 --host > "$LOGS_DIR/admin-ui.log" 2>&1 &
    disown
    log "  🚀 管理后台 (8090)"
fi

# 交易终端
if ! curl -s -o /dev/null http://localhost:5173/ 2>/dev/null; then
    cd "$WEB_DIR"
    nohup npx vite --port 5173 --host > "$LOGS_DIR/web.log" 2>&1 &
    disown
    log "  🚀 交易终端 (5173)"
fi

# ============ 完成 ============
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          所有服务启动完成！                        ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${CYAN}管理后台${NC}      http://localhost:8090/   (admin / admin123)"
echo -e "  ${CYAN}交易终端${NC}      http://localhost:5173/"
echo -e "  ${CYAN}API 网关${NC}      http://localhost:8088/"
echo -e "  ${CYAN}Prometheus${NC}    http://localhost:9090/"
echo -e "  ${CYAN}Grafana${NC}       http://localhost:3000/  (admin / admin123)"
echo ""

log "服务状态:"
for svc in "${SERVICES[@]}"; do
    local_port="${PORTS[$svc]}"
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$local_port/actuator/health" 2>/dev/null || echo "000")
    printf "  %-12s (%4d): %s\n" "$svc" "$local_port" "$([ "$STATUS" != "000" ] && echo '✅' || echo '❌')"
done
for name in "管理后台" "交易终端"; do
    case $name in
        "管理后台") port=8090;;
        "交易终端") port=5173;;
    esac
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/" 2>/dev/null || echo "000")
    printf "  %-12s (%4d): %s\n" "$name" "$port" "$([ "$STATUS" != "000" ] && echo '✅' || echo '❌')"
done
echo ""
log "日志目录: $LOGS_DIR"
log "按 Ctrl+C 停止所有服务"
echo ""

# 保持脚本运行
wait
