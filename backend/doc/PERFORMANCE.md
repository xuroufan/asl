# 期货交易平台 — 性能测试计划

## 1. 测试目标

| 指标 | 目标 P99 | 测量方法 |
|------|----------|----------|
| API 响应时间 | < 500ms | `ab` / k6 |
| 撮合延迟 | < 50ms | 内部计时 |
| 吞吐量 | > 1000 TPS | `ab` / k6 |
| 并发用户 | > 100 | `ab -c 100` |

## 2. 测试场景

### 2.1 行情 API（无状态，缓存友好）
```bash
# 测试命令
ab -n 10000 -c 50 http://localhost:8088/api/v1/market/symbols
```
预期: > 5000 req/s, P99 < 50ms

### 2.2 用户注册（写操作）
```bash
ab -n 500 -c 10 -T application/json -p register.json \
  http://localhost:8088/api/v1/auth/register
```
预期: > 100 req/s, P99 < 200ms

### 2.3 登录（读 + JWT 生成）
```bash
ab -n 1000 -c 20 -T application/json -p login.json \
  http://localhost:8088/api/v1/auth/login
```
预期: > 200 req/s, P99 < 150ms

### 2.4 资金余额查询（DB 查询）
```bash
ab -n 500 -c 10 -H "Authorization: Bearer TOKEN" \
  "http://localhost:8088/api/v1/fund/balance?userId=1"
```
预期: > 50 req/s, P99 < 300ms

### 2.5 全链路混合（模拟真实用户）
- 1 秒内: 注册 → 登录 → 查询行情 → 入金 → 查询余额
- 持续 5 分钟

## 3. 性能基线（当前值）

| 场景 | 请求/秒 | P99 延迟 | 测试日期 |
|------|---------|----------|----------|
| 行情查询 | — | — | — |
| 用户注册 | — | — | — |
| 用户登录 | — | — | — |
| 资金余额 | — | — | — |

*运行 `bash load-test.sh` 生成基线数据*

## 4. 工具

### ab (Apache Bench)
```bash
# 基本用法
ab -n 1000 -c 10 http://localhost:8088/api/v1/market/symbols

# POST 请求
ab -n 500 -c 10 -T application/json -p body.json http://localhost:8088/api/v1/auth/login

# 带 Header
ab -n 500 -c 10 -H "Authorization: Bearer TOKEN" http://localhost:8088/api/v1/account/info
```

### k6（推荐，需手动安装）
```bash
# 安装
brew install k6

# 运行
k6 run k6-script.js

# 输出: HTTP请求延迟分布、吞吐量、错误率
```

## 5. k6 脚本（需安装 k6）

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 热身
    { duration: '1m', target: 50 },   // 爬坡
    { duration: '2m', target: 100 },  // 峰值
    { duration: '30s', target: 0 },   // 降负载
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8088';

export default function () {
  // 1. 行情查询（公开）
  const symbols = http.get(`${BASE_URL}/api/v1/market/symbols`);
  check(symbols, { 'symbols ok': (r) => r.status === 200 });

  // 2. 注册新用户
  const ts = Date.now();
  const reg = http.post(`${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ username: `perf_${ts}`, password: 'Test1234!', email: `perf_${ts}@test.com` }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(reg, { 'register ok': (r) => r.status === 200 });

  // 3. 登录
  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: `perf_${ts}`, password: 'Test1234!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(login, { 'login ok': (r) => r.status === 200 });

  sleep(1);
}
```

## 6. 运行方式

```bash
# 1. 确保服务已启动
bash smoke-test.sh

# 2. 运行负载测试
bash load-test.sh

# 3. 查看结果
cat load-test-results/*.txt | grep -E "Requests per second|Time per request|Failed requests"

# 4. 记录基线到 PERFORMANCE.md
```

## 7. 性能瓶颈分析

| 瓶颈 | 症状 | 解决方案 |
|------|------|----------|
| 数据库连接池 | P99 > 1s | 增加 pool size / 加索引 |
| JVM GC | 响应时间波动大 | 调整 GC 参数 / 增加堆内存 |
| 网关限流 | 4xx/5xx 错误 | 调整限流参数 / 增加副本 |
| Nacos 服务发现 | 首次请求慢 | 预创建连接 / 使用本地缓存 |
| 网络延迟 | 所有请求均慢 | 服务间调用改为内网 / 同机部署 |

## 8. 容量规划建议

| 日交易量 | 建议配置 |
|---------|----------|
| < 10,000 笔 | 8个微服务各 256MB, 单机 |
| 10K - 100K | 各 512MB, 网关 2副本 |
| 100K - 1M | 各 1GB, 全服务 2副本, DB 主从 |
| > 1M | 各 2GB, K8s 自动扩缩, DB 集群 |
