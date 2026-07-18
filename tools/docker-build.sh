#!/bin/bash
# ================================================
# ASL 期货平台 — Docker 镜像构建 & GHCR 推送
# 依赖: Docker Desktop, 已登录 GHCR
# ================================================

BASE="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$BASE/backend"
REGISTRY="ghcr.io/xuroufan/asl"
DATE=$(date '+%Y%m%d')
GIT_HASH=$(cd "$BASE" && git log --format=%h -1 2>/dev/null || echo "unknown")

# 服务定义: 名称:端口
SERVICES="order:8081 matching:8082 account:8083 fund:8084 risk:8085 market:8086 settlement:8087 push:8093 admin:8099 gateway:8088"

JAVA21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/java"

log() { echo "[$(date '+%H:%M:%S')] $1"; }

# ---- 1. 重新编译 JAR ----
log "=== 编译 JAR ==="
cd "$BACKEND" || exit 1
mvn package -Dmaven.test.skip=true -T 4 -q 2>&1 | tail -3
log "JAR 编译完成"

# ---- 2. 构建 Docker 镜像 ----
log "=== 构建镜像 ==="
mkdir -p "$BASE/tmp/docker-build"

for svc_spec in $SERVICES; do
  IFS=':' read -r svc port <<< "$svc_spec"
  JAR_FILE="$BACKEND/futures-$svc/target/futures-$svc-1.0.0-SNAPSHOT.jar"
  
  if [ ! -f "$JAR_FILE" ]; then
    log "  ⚠️ 跳过 $svc: JAR 不存在"
    continue
  fi

  BUILD_DIR="$BASE/tmp/docker-build/$svc"
  mkdir -p "$BUILD_DIR"

  # 创建简单 Dockerfile
  cat > "$BUILD_DIR/Dockerfile" << DOCKER
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY futures-$svc-1.0.0-SNAPSHOT.jar app.jar
RUN mkdir -p /app/logs && addgroup -S futures && adduser -S futures -G futures
RUN chown -R futures:futures /app
USER futures
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -sf http://localhost:$port/actuator/health || exit 1
EXPOSE $port
ENV JAVA_OPTS="-server -Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT ["sh", "-c", "java \${JAVA_OPTS} -jar app.jar --server.port=$port"]
DOCKER

  cp "$JAR_FILE" "$BUILD_DIR/"

  IMAGE="$REGISTRY/$svc"
  log "  构建 $svc ($port) -> $IMAGE ..."
  docker build -t "$IMAGE:latest" -t "$IMAGE:$DATE" -t "$IMAGE:$GIT_HASH" "$BUILD_DIR" 2>&1 | tail -1
  log "  ✅ $svc 构建完成"
done

# ---- 3. 推送到 GHCR ----
log "=== 推送到 GHCR ==="
for svc_spec in $SERVICES; do
  IFS=':' read -r svc port <<< "$svc_spec"
  IMAGE="$REGISTRY/$svc"
  log "  推送 $IMAGE:latest ..."
  docker push "$IMAGE:latest" 2>&1 | tail -1
  log "  ✅ $svc 推送完成"
done

# ---- 4. 清理 ----
rm -rf "$BASE/tmp/docker-build"
log "=== 全部完成 ==="
echo ""
echo "镜像列表:"
for svc_spec in $SERVICES; do
  IFS=':' read -r svc _ <<< "$svc_spec"
  echo "  ghcr.io/xuroufan/asl/$svc:latest"
done
