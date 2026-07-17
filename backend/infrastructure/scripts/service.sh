#!/bin/bash
# ============================================================
# Futures Trading Platform - Microservice Startup Script
# with SkyWalking APM Agent Integration
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SKYWALKING_AGENT_DIR="$PROJECT_DIR/infrastructure/skywalking/skywalking-agent"

# Default OAP address (override with SW_OAP_ADDRESS env)
SW_OAP_ADDRESS="${SW_OAP_ADDRESS:-localhost:11800}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Validate SkyWalking agent exists
if [ ! -f "$SKYWALKING_AGENT_DIR/skywalking-agent.jar" ]; then
    echo -e "${RED}[ERROR]${NC} SkyWalking agent not found at: $SKYWALKING_AGENT_DIR/skywalking-agent.jar"
    exit 1
fi

# Validate services directory
SERVICES_DIR="$PROJECT_DIR"
if [ ! -d "$SERVICES_DIR/futures-gateway" ]; then
    echo -e "${RED}[ERROR]${NC} Services directory not found at: $SERVICES_DIR"
    exit 1
fi

# Function to start a service
start_service() {
    local svc_name="$1"
    local jar_path="$2"
    local port="${3:-}"
    
    echo -e "\n${GREEN}[Starting]${NC} $svc_name (port: ${port:-auto})..."
    
    # Build SkyWalking agent JVM args
    local agt_args="-javaagent:${SKYWALKING_AGENT_DIR}/skywalking-agent.jar"
    agt_args="$agt_args -Dskywalking.agent.service_name=futures-${svc_name}"
    agt_args="$agt_args -Dskywalking.collector.backend_service=${SW_OAP_ADDRESS}"
    agt_args="$agt_args -Dskywalking.plugin.toolkit.log.grpc.reporter.server_host=$(echo ${SW_OAP_ADDRESS} | cut -d: -f1)"
    agt_args="$agt_args -Dskywalking.plugin.toolkit.log.grpc.reporter.server_port=$(echo ${SW_OAP_ADDRESS} | cut -d: -f2)"
    
    # Application args
    local app_args="-Xmx256m -Xms128m"
    if [ -n "$port" ]; then
        app_args="$app_args --server.port=${port}"
    fi
    
    # Start the service
    cd "$SERVICES_DIR"
    local pid_file="/tmp/futures-${svc_name}.pid"
    
    nohup java $app_args $agt_args -jar "$jar_path" > "/tmp/fm-${svc_name}.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$pid_file"
    
    echo -e "  PID: ${YELLOW}$pid${NC}"
    echo -e "  Log: ${YELLOW}/tmp/fm-${svc_name}.log${NC}"
}

# Function to stop a service
stop_service() {
    local svc_name="$1"
    local pid_file="/tmp/futures-${svc_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping $svc_name (PID: $pid)..."
            kill "$pid" 2>/dev/null
            rm -f "$pid_file"
        else
            echo "$svc_name not running (stale PID file)"
            rm -f "$pid_file"
        fi
    fi
}

# Main command
case "${1:-help}" in
    start)
        echo "========================================"
        echo "  Futures Platform - Starting Services  "
        echo "========================================"
        echo "SkyWalking OAP: $SW_OAP_ADDRESS"
        echo "Agent: $SKYWALKING_AGENT_DIR"
        
        start_service "gateway"    "futures-gateway/target/futures-gateway-1.0.0-SNAPSHOT.jar" 8088
        start_service "order"      "futures-order/target/futures-order-1.0.0-SNAPSHOT.jar" 8081
        start_service "matching"   "futures-matching/target/futures-matching-1.0.0-SNAPSHOT.jar" 8082
        start_service "account"    "futures-account/target/futures-account-1.0.0-SNAPSHOT.jar" 8083
        start_service "fund"       "futures-fund/target/futures-fund-1.0.0-SNAPSHOT.jar" 8084
        start_service "risk"       "futures-risk/target/futures-risk-1.0.0-SNAPSHOT.jar" 8085
        start_service "market"     "futures-market/target/futures-market-1.0.0-SNAPSHOT.jar" 8086
        start_service "settlement" "futures-settlement/target/futures-settlement-1.0.0-SNAPSHOT.jar" 8087
        
        echo -e "\n${GREEN}[Done]${NC} All services started!"
        echo "Check status: ./scripts/service.sh status"
        echo "View logs:    tail -f /tmp/fm-*.log"
        ;;
    stop)
        echo "Stopping all services..."
        for svc in settlement risk fund account matching order market gateway; do
            stop_service "$svc"
        done
        echo "All services stopped."
        ;;
    status)
        echo "=== Service Status ==="
        for svc in gateway order matching account fund risk market settlement; do
            pid_file="/tmp/futures-${svc}.pid"
            if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
                echo -e "${GREEN}[RUNNING]${NC} futures-${svc} (PID: $(cat $pid_file))"
            else
                echo -e "${RED}[STOPPED]${NC} futures-${svc}"
            fi
        done
        ;;
    *)
        echo "Usage: $0 {start|stop|status}"
        echo ""
        echo "  start   - Start all services with SkyWalking agent"
        echo "  stop    - Stop all services"
        echo "  status  - Show service status"
        exit 1
        ;;
esac
