#!/bin/bash
# ================================================
#  ASL 期货交易平台 — 一键启动脚本 v2
#  修复: 工作目录错误、健康检查、Redis 冲突
#  依赖: JDK 21, Docker, tmux
# ================================================

set -e

JAVA21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/java"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"
BACKEND_DIR="$PROJECT_DIR/backend"

# 健康修复配置
CFG="--spring.config.additional-location=file:$PROJECT_DIR/config/health-fix.yml"
# Nacos + Seata 禁用（防止后台线程崩溃）
NOS="--spring.cloud.nacos.discovery.enabled=false --spring.cloud.nacos.config.enabled=false --seata.enabled=false --management.health.enabled.redis.enabled=false"
REX="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration"
# Fund/Push 额外排除
FPX="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"

mkdir -p "$LOG_DIR"

RED='[0;31m'; GREEN='[0;32m'; YELLOW='[1;33m'; NC='[0m'
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
    tmux kill-session -t "$s"  || true
done

# ============ 启动 Docker 基础设施 ============
info "启动 Docker (MySQL, Redis, Prometheus, Grafana)..."
cd "$PROJECT_DIR"
docker compose -f docker/dev-infra.yml up -d 

info "等待 MySQL 就绪..."
for i in $(seq 1 30); do
    docker ps --format '{{.Names}} {{.Status}}'  | grep -q "asl-mysql.*healthy" && break || true
    sleep 2
done

info "等待 Redis 就绪..."
for i in $(seq 1 15); do
    docker ps --format '{{.Names}} {{.Status}}'  | grep -q "asl-redis.*healthy" && break || true
    sleep 2
done

# ============ 启动 9 个微服务 ============
info "启动 9 个后端微服务..."

# Order (8081)
tmux new-session -d -s order "cd $PROJECT_DIR && $JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-order/target/futures-order-1.0.0-SNAPSHOT.jar --rocketmq.enabled=false --spring.cloud.nacos.enabled=false $CFG 2>&1 | tee $LOG_DIR/order.log"
info "  order(8081)"

# Admin (8099)
tmux new-session -d -s admin "cd $PROJECT_DIR && $JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-admin/target/futures-admin-1.0.0-SNAPSHOT.jar $CFG 2>&1 | tee $LOG_DIR/admin.log"
info "  admin(8099)"

# matching, account, risk, market, settlement — 带健康修复
for svc in matching account risk market settlement; do
    tmux new-session -d -s "svc-$svc" "cd $PROJECT_DIR && $JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-$svc/target/futures-$svc-1.0.0-SNAPSHOT.jar $NOS --spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration $CFG 2>&1 | tee $LOG_DIR/$svc.log"
    info "  $svc"
done

# Fund (8084) — 排除 RocketMQ + DataSource
tmux new-session -d -s svc-fund "cd $PROJECT_DIR && $JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-fund/target/futures-fund-1.0.0-SNAPSHOT.jar $NOS $FPX $CFG 2>&1 | tee $LOG_DIR/fund.log"
info "  fund(8084)"

# Push (8093) — 排除 RocketMQ + DataSource
tmux new-session -d -s svc-push "cd $PROJECT_DIR && $JAVA21 -Xmx256m -jar $BACKEND_DIR/futures-push/target/futures-push-1.0.0-SNAPSHOT.jar $NOS $FPX $CFG 2>&1 | tee $LOG_DIR/push.log"
info "  push(8093)"

# Gateway (8088)
tmux new-session -d -s gateway "cd $PROJECT_DIR && $JAVA21 -Xmx96m -jar $BACKEND_DIR/futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar 2>&1 | tee $LOG_DIR/gateway.log"
info "  gateway(8088)"

# ============ 启动前端 ============
info "启动前端..."
tmux new-session -d -s admin-ui "cd $PROJECT_DIR/admin-ui && node_modules/.bin/vite --port 8090 --host 2>&1 | tee $LOG_DIR/admin-ui.log"
info "  Admin UI (8090)"
tmux new-session -d -s web "cd $PROJECT_DIR/web && node_modules/.bin/vite --port 5173 --host 2>&1 | tee $LOG_DIR/web.log"
info "  Web (5173)"

# ============ 等待就绪 ============
info "等待服务就绪 (30s)..."
sleep 30

# ============ 验证 ============
echo ""
info "===== 系统状态 ====="
for pair in "8081:order" "8082:matching" "8083:account" "8084:fund" "8085:risk" "8086:market" "8087:settlement" "8093:push" "8099:admin"; do
    p="${pair%%:*}"; n="${pair##*:}"
    st=$(curl -s -m3 http://localhost:$p/actuator/health  | python3 -c "import sys,json;print(json.load(sys.stdin).get('status','?'))"  || echo "starting")
    echo "  $n(:$p) → $st"
done

echo ""
info "Gateway API 测试:"
curl -s -m3 http://localhost:8088/api/v1/market/symbols | python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  Market: {d["code"]} ({len(d["data"])} symbols)')" 
curl -s -m3 -H 'X-User-Id: 1' http://localhost:8088/api/v1/trade/positions | python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  Positions: {d["code"]} ({len(d["data"])})')" 
curl -s -m3 http://localhost:8088/actuator/health | python3 -c "import sys,json;print(f'  Gateway: {json.load(sys.stdin)["status"]}')" 

echo ""
info "===== 访问地址 ====="
echo "  Admin UI:  http://localhost:8090"
echo "  Web:       http://localhost:5173"
echo "  Grafana:   http://localhost:3000 (admin/admin)"
echo "  Prometheus: http://localhost:9090"
echo ""
info "命令: bash start.sh 重启全部 | bash start.sh stop 停止全部 | tmux ls 查看会话"
