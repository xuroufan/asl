# 1. JVM 参数标准化

## 1.1 统一 JAVA_OPTS 模板

```dockerfile
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
    -XX:ErrorFile=/app/logs/hs_err_pid%p.log \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+PrintCommandLineFlags \
    -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags::filecount=10,filesize=100M \
    -Djava.security.egd=file:/dev/./urandom"
```

## 1.2 各服务容器资源配置

```yaml
# Key modifications from current Dockerfile:
# 1. MaxGCPauseMillis: 50 → 200 (more realistic, prevents GC thrashing)
# 2. Added G1HeapRegionSize, G1NewSizePercent, G1ReservePercent
# 3. Added ExitOnOutOfMemoryError
# 4. GC log: 10 files × 100MB (was 10MB)
# 5. HeapDumpOnOutOfMemoryError - already configured
```

## 1.3 K8s Deployment 配置

```yaml
# --- matching 服务 ---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: futures-matching
  namespace: futures
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: matching
        image: futures/matching:latest
        ports:
        - containerPort: 8082
        env:
        - name: JAVA_HEAP
          value: "1536m"
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: NACOS_ADDR
          value: "nacos:8848"
        resources:
          requests:
            memory: "1.5Gi"
            cpu: "2"
          limits:
            memory: "2Gi"
            cpu: "4"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 30
```

## 1.4 容器感知 JVM 参数（适用于 K8s）

当容器未被配置资源限制时，JVM 默认使用宿主机总内存作为 `-XX:+UseContainerSupport` 的基准。
在 K8s 环境中必须显式设置 `-XX:ActiveProcessorCount` 和 `-XX:MaxRAMPercentage`：

```dockerfile
# 替代方案：使用百分比而非固定值
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=80.0 \
    -XX:InitialRAMPercentage=80.0 \
    -XX:MinRAMPercentage=80.0 \
    -XX:ActiveProcessorCount=4 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:G1HeapRegionSize=4m \
    -XX:+UseStringDeduplication \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags::filecount=10,filesize=100M"
```

## 1.5 对比当前配置与优化后配置

| 参数 | 当前值 | 优化值 | 说明 |
|------|--------|--------|------|
| `-XX:MaxGCPauseMillis` | 50 | **200** | 50过于激进导致频繁GC，200兼顾吞吐 |
| `-XX:InitiatingHeapOccupancyPercent` | 70 | **45** | 70触发太晚易导致 Full GC |
| `-XX:G1HeapRegionSize` | 无 | **4m** | 显式指定Region大小 |
| `-XX:G1NewSizePercent` | 无 | **10** | 保障年轻代最小占比 |
| `-XX:+ExitOnOutOfMemoryError` | 无 | **有** | OOM时自动退出，K8s自动重启 |
| `-XX:+PrintCommandLineFlags` | 无 | **有** | 启动时打印实际生效的JVM参数 |
| GC日志文件大小 | 10m | **100M** | 减少日志轮转频率 |
| GC日志格式 | `time,tags` | **time,uptime,level,tags** | 增加uptime便于分析 |
