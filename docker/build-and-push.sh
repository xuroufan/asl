#!/bin/bash
set -e

VERSION="1.0.1"
REGISTRY="ghcr.io/xuroufan/asl"
DIR="$(cd "$(dirname "$0")/.." && pwd)"

declare -A SERVICES
SERVICES["futures-gateway"]="8088"
SERVICES["futures-order"]="8081"
SERVICES["futures-matching"]="8082"
SERVICES["futures-account"]="8083"
SERVICES["futures-fund"]="8084"
SERVICES["futures-risk"]="8085"
SERVICES["futures-market"]="8086"
SERVICES["futures-settlement"]="8087"
SERVICES["futures-push"]="8093"
SERVICES["futures-admin"]="8099"

for SERVICE in "${!SERVICES[@]}"; do
    PORT="${SERVICES[$SERVICE]}"
    EXTRA=""
    if [ "$SERVICE" = "futures-fund" ] || [ "$SERVICE" = "futures-push" ]; then
        EXTRA="org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"
    fi
    
    echo ""
    echo "=============================="
    echo "Building: $SERVICE (port $PORT)"
    echo "Extra: ${EXTRA:-none}"
    echo "=============================="
    
    cd "$DIR"
    docker build -f docker/services/Dockerfile \
        --build-arg SERVICE=$SERVICE \
        --build-arg PORT=$PORT \
        --build-arg "EXTRA_EXCLUDES=$EXTRA" \
        -t $REGISTRY/$SERVICE:$VERSION \
        -t $REGISTRY/$SERVICE:latest \
        . 2>&1 | tail -5
    
    echo "Pushing $SERVICE..."
    docker push $REGISTRY/$SERVICE:$VERSION 2>&1 | tail -3
    docker push $REGISTRY/$SERVICE:latest 2>&1 | tail -3
    echo "$SERVICE done!"
done

echo ""
echo "====================================="
echo "All 10 images built and pushed!"
echo "====================================="
docker images | grep ghcr.io/xuroufan/asl
