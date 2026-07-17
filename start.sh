#!/bin/bash
# ================================================
#  ASL 期货交易平台 — 一键启动脚本
#  启动所有微服务、Gateway、前端、Docker 基础设施
#  使用 JDK 21（避免 JDK 26 的 Connection.isValid 问题）
# ================================================

set -e

# ============ 配置 ============
JAVA21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/java"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"
BACKEND_DIR="$PROJECT_DIR/backend"
# HEALTH check flags (experimental): "--management.health.nacos-discovery.enabled=false --management.health.seata.enabled=false"
EXCLUDE_FLAGS="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"

mkdir -p "$LOG_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============ 前置检查 ============
info "检查依赖..."
for cmd in java tmux docker; do
    command -v $cmd >/dev/null 2>&1 || { error "需要 $cmd"; exit 1; }
done

# ============ 停止已有服务 ============
info "停止已有服务..."
for s in gateway order admin svc-matching svc-account svc-fund svc-risk svc-market svc-settlement svc-push admin-ui web; do
    tmux kill-session -t "$s" 2>/dev/null || true
done

# ============ 启动 Docker 基础设施 ============
info "启动 Docker (MySQL, Redis, Prometheus, Grafana)..."
cd "$PROJECT_DIR" && docker compose -f docker/dev-infra.yml up -d 2>/dev/null || docker-compose -f docker/dev-infra.yml up -d
info "等待 MySQL 就绪..."
for i in $(seq 1 30); do
    docker ps --format '{{.Names}} {{.Status}}' 2>/dev/null | grep -q "healthy" && break
    sleep 2
done

# ============ 启动 9 个微服务 ============
info "启动 9 个后端微服务..."

# Order (8081) — 禁用 RocketMQ + Nacos
tmux new-session -d -s order "$JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-order/target/futures-order-1.0.0-SNAPSHOT.jar --rocketmq.enabled=false --spring.cloud.nacos.enabled=false 2>&1 | tee $LOG_DIR/order.log"
info "  order(8081)"

# Admin (8099)
tmux new-session -d -s admin "$JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-admin/target/futures-admin-1.0.0-SNAPSHOT.jar 2>&1 | tee $LOG_DIR/admin.log"
info "  admin(8099)"

# matching, account, risk, market, settlement
for svc in matching account risk market settlement; do
    tmux new-session -d -s "svc-$svc" "$JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-$svc/target/futures-$svc-1.0.0-SNAPSHOT.jar 2>&1 | tee $LOG_DIR/$svc.log"
    info "  $svc"
done

# Fund (8084)
tmux new-session -d -s svc-fund "$JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-fund/target/futures-fund-1.0.0-SNAPSHOT.jar $EXCLUDE_FLAGS 2>&1 | tee $LOG_DIR/fund.log"
info "  fund(8084)"

# Push (8093)
tmux new-session -d -s svc-push "$JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-push/target/futures-push-1.0.0-SNAPSHOT.jar $EXCLUDE_FLAGS 2>&1 | tee $LOG_DIR/push.log"
info "  push(8093)"

# Gateway (8088)
tmux new-session -d -s gateway "$JAVA21 -Xmx96m -jar $BACKEND_DIR/futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar 2>&1 | tee $LOG_DIR/gateway.log"
info "  gateway(8088)"

# ============ 启动前端 ============
info "启动前端..."
tmux new-session -d -s admin-ui "cd $PROJECT_DIR/admin-ui && node_modules/.bin/vite --port 8090 --host 2>&1 | tee $LOG_DIR/admin-ui.log"
info "  Admin UI (8090)"
tmux new-session -d -s web "cd $PROJECT_DIR/web && node_modules/.bin/vite --port 5173 --host 2>&1 | tee $LOG_DIR/web.log"
info "  Web (5173)"

# ============ 等待就绪 ============
info "等待服务就绪..."
sleep 25

# ============ 验证 ============
echo ""
info "===== 系统状态 ====="
for pair in "8081:order" "8082:matching" "8083:account" "8084:fund" "8085:risk" "8086:market" "8087:settlement" "8093:push" "8099:admin"; do
    p="$(echo $pair | cut -d: -f1)"; n="$(echo $pair | cut -d: -f2)"
    st=$(curl -s http://localhost:$p/actuator/health 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin).get('status','?'))" 2>/dev/null || echo "starting")
    echo "  $n(:$p) → $st"
done

echo ""
info "Gateway API 测试:"
curl -s http://localhost:8088/api/v1/market/symbols | python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  Market: code={d["code"]}, {len(d["data"])} symbols')" 2>/dev/null
curl -s -H 'X-User-Id: 1' http://localhost:8088/api/v1/trade/positions | python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  Positions: code={d["code"]}, {len(d["data"])} positions')" 2>/dev/null
curl -s http://localhost:8088/actuator/health | python3 -c "import sys,json;print(f'  Gateway: {json.load(sys.stdin)["status"]}')" 2>/dev/null

echo ""
info "Dashboard URLs:"
echo "  Admin UI:  http://localhost:8090"
echo "  Web:       http://localhost:5173"
echo "  Grafana:   http://localhost:3000 (admin/admin)"
echo "  Prometheus: http://localhost:9090"
echo ""
info "控制: bash start.sh 重启全部 | tmux ls 查看全部会话"
