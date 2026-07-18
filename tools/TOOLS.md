# 运维工具

| 工具 | 说明 |
|------|------|
| [backup.sh](backup.sh) | 数据库备份（MySQL + Redis，保留 7 天）|
| [benchmark.sh](benchmark.sh) | 撮合引擎压力测试 |
| [docker-build.sh](docker-build.sh) | Docker 镜像构建 & GHCR 推送 |
| [api-docs.html](api-docs.html) | API 文档集中入口 |

## backup.sh

```bash
bash tools/backup.sh
```

备份 `futures_*` 全部数据库和 Redis RDB 到 `backups/` 目录。
建议每天凌晨 3:00 执行：
```
0 3 * * * /path/to/tools/backup.sh
```

## benchmark.sh

```bash
# 简单压测（10 并发，50 请求）
bash tools/benchmark.sh 10 50

# 含模拟下单
bash tools/benchmark.sh 10 50 --orders
```

测试端点:
- `GET /actuator/health`
- `GET /api/v1/matching/depth`
- `GET /api/v1/matching/price`
- `POST /api/v1/matching/place` (仅 `--orders`)

## docker-build.sh

```bash
bash tools/docker-build.sh
```

重新编译 JAR → 构建 10 个 Docker 镜像 → 推送到 `ghcr.io/xuroufan/asl/<service>:latest`

## 压力测试报告

最新报告: [benchmark_report.md](benchmark_report.md)

### 2026-07-18 测试结果

| 测试 | 成功率 | 平均延迟 | P95 |
|------|--------|----------|-----|
| 健康检查 | 100% | 16ms | 21ms |
| 订单簿深度 | 100% | 13ms | 16ms |
| 中间价 | 100% | 15ms | 18ms |
| 模拟下单 | 100% | 117ms | 146ms |
