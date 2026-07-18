# API 文档

## 网关地址

```
开发环境: http://localhost:8088
所有 REST API 通过 API 网关统一入口访问
```

## 认证

### 登录获取 Token

```bash
POST /api/v1/admin/auth/login
Content-Type: application/json

{"username": "admin", "password": "admin123"}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "userId": 1, "username": "admin", "nickname": "管理员" }
  }
}
```

### 请求鉴权

```
Authorization: Bearer <accessToken>
```

所有需认证的接口均在请求头携带上述 Token。网关和业务服务使用 HMAC-SHA256 验证。

## 网关路由表

| 路由 | 目标服务 | 端口 |
|------|----------|------|
| `/api/v1/admin/**` | Admin | 8099 |
| `/api/v1/order/**` | Order | 8081 |
| `/api/v1/trade/**` | Order | 8081 |
| `/api/v1/position/**` | Order | 8081 |
| `/api/v1/matching/**` | Matching | 8082 |
| `/api/v1/account/**` | Account | 8083 |
| `/api/v1/auth/**` | Account | 8083 |
| `/api/v1/fund/**` | Fund | 8084 |
| `/api/v1/risk/**` | Risk | 8085 |
| `/api/v1/market/**` | Market | 8086 |
| `/api/v1/settlement/**` | Settlement | 8087 |
| `/api/v1/push/**` | Push | 8093 |
| `/ws/**` | Push (WS) | 8093 |

## WebSocket 协议

### 连接

```javascript
const ws = new WebSocket('ws://localhost:8088/ws/{userId}')
```

### 客户端消息

```json
// 订阅行情
{"action": "subscribe", "type": "market", "symbols": ["HSI"]}

// 订阅订单更新
{"action": "subscribe", "type": "order"}

// 取消订阅
{"action": "unsubscribe", "type": "market", "symbols": ["HSI"]}

// 心跳
{"action": "pong"}
```

### 服务端推送

```json
// 实时报价
{"type": "quote", "symbol": "HSI", "price": 4500.50, "volume": 1234}

// 实时成交
{"type": "trade", "symbol": "HSI", "price": 4500.00, "quantity": 10, "timestamp": 1784305123456}

// 盘口更新
{"type": "depth", "symbol": "HSI", "bids": [[4499.00, 100], ...], "asks": [[4501.00, 200], ...]}

// 订单状态更新
{"type": "order", "orderId": "ORD-xxx", "status": "FILLED", "filledVolume": 10}
```

## 关键接口

### 订单

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/order/place` | 下单 | ✅ |
| POST | `/api/v1/order/cancel` | 撤单 | ✅ |
| GET | `/api/v1/order/current` | 当前委托 | ✅ |
| GET | `/api/v1/order/history` | 历史委托 | ✅ |

### 撮合引擎

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/matching/place` | 提交撮合 | ❌ |
| POST | `/api/v1/matching/cancel` | 撤单 | ❌ |
| GET | `/api/v1/matching/depth` | 订单簿深度 | ❌ |
| GET | `/api/v1/matching/price` | 中间价 | ❌ |

### 行情

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/market/all-quotes` | 所有合约报价 | ✅ |
| GET | `/api/v1/market/quote` | 单合约报价 | ✅ |
| GET | `/api/v1/market/kline` | K线数据 | ✅ |
| GET | `/api/v1/market/depth` | 盘口深度 | ✅ |
| GET | `/api/v1/market/symbols` | 合约列表 | ✅ |

### 账户

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/account/balance` | 账户余额 | ✅ |
| GET | `/api/v1/account/transactions` | 交易记录 | ✅ |

### 管理后台 (112 endpoints)

完整交互文档：http://localhost:8099/doc.html

| 模块 | 路径前缀 | 接口数 |
|------|---------|--------|
| 认证 | `/api/v1/admin/auth` | 8 |
| 系统管理 | `/api/v1/admin/system` | ~40 |
| 监控 | `/api/v1/admin/monitor` | ~20 |
| 风控 | `/api/v1/admin/risk` | ~12 |
| CRM | `/api/v1/admin/crm` | ~15 |
| 财务 | `/api/v1/admin/finance` | ~10 |
| 运维 | `/api/v1/admin/ops` | ~12 |

## 健康检查

```bash
GET /actuator/health
```

所有微服务均暴露统一的健康端点。Gateway 会透传到各个服务。

```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    "db": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```
