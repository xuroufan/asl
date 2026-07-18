# ============================================================
# Futures Platform - Multi-stage Dockerfile
# Build: docker build --build-arg SERVICE=gateway -t futures/gateway .
# ============================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder

ARG SERVICE

WORKDIR /build

COPY . .

RUN mvn package -pl futures-${SERVICE} -am -DskipTests --no-transfer-progress -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre

ARG SERVICE
ARG SERVICE_PORT=8080

LABEL maintainer="futures-platform" service="futures-${SERVICE}"

RUN groupadd -r futures && useradd -r -g futures -m -d /app futures
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /build/futures-${SERVICE}/target/futures-${SERVICE}-*.jar app.jar

RUN mkdir -p /app/logs && chown -R futures:futures /app

USER futures

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -sf http://localhost:${SERVICE_PORT}/actuator/health || exit 1

EXPOSE ${SERVICE_PORT}

# ============================================================
# JVM 优化参数 (G1GC + 容器感知 + 字符串去重)
# ============================================================
# 内存比例建议 (K8s limit = 容器内存上限):
#   通用服务: JAVA_HEAP=512m  → limit=768Mi
#   高负载服务(matching): JAVA_HEAP=1g → limit=1.5Gi
# ============================================================
ENV JAVA_HEAP=512m \
    NACOS_ADDR="nacos:8848" \
    NACOS_NAMESPACE="futures-platform" \
    SPRING_PROFILES_ACTIVE="prod"

ENV JAVA_OPTS="-server \
    -Xms${JAVA_HEAP} -Xmx${JAVA_HEAP} \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:G1HeapRegionSize=4m \
    -XX:G1NewSizePercent=10 \
    -XX:G1ReservePercent=15 \
    -XX:+UseStringDeduplication \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+PrintCommandLineFlags \
    -XX:ErrorFile=/app/logs/hs_err_pid%p.log \
    -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags::filecount=10,filesize=100M \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar --server.port=${SERVICE_PORT}"]
