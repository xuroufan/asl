# 系统架构

## 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    Client Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  │ Android  │  │ Web UI   │  │ Admin UI (Vue 3)   │ │
│  │  Kotlin  │  │  React   │  │  Element Plus      │ │
│  │  Compose │  │ Tailwind │  │  ECharts            │ │
│  └────┬─────┘  └────┬─────┘  └──────────┬─────────┘ │
│       │             │                    │            │
│       └─────────────┼────────────────────┘            │
│                     │ HTTP/WS (JWT)                   │
└─────────────────────┼────────────────────────────────┘
                      │
               ┌──────▼──────┐
               │  API Gateway │  Spring Cloud Gateway
               │  localhost   │  JWT Auth, Rate Limit
               │  :8088       │
               └──┬───┬───┬──┘
    ┌─────────────┘   │   └──────────────┐
    ▼                 ▼                  ▼
┌─────────┐    ┌──────────┐    ┌─────────────┐
│  Auth   │    │  Order   │    │  Matching   │
│  Account│    │  Trade   │    │  Disruptor  │
│  :8083  │    │  :8081   │    │  :8082      │
└────┬────┘    └────┬─────┘    └──────┬──────┘
     │              │                  │
     │              │           ┌──────▼──────┐
     │              │           │   Market    │
     │              │           │   :8086     │
     │              │           └─────────────┘
     │              │
┌────▼────┐   ┌────▼─────┐    ┌──────────────┐
│  Fund   │   │  Risk    │    │ Settlement   │
│  :8084  │   │  :8085   │    │  :8087       │
└─────────┘   └──────────┘    └──────────────┘

┌──────────┐   ┌──────────┐
│  Admin   │   │  Push    │
│  :8099   │   │  WS      │
│          │   │  :8093   │
└──────────┘   └──────────┘

┌──────────────────────────────────────────────────────────┐
│                    Data Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐              │
│  │  MySQL   │  │  Redis   │  │ RocketMQ  │              │
│  │  8.0     │  │  7       │  │           │              │
│  └──────────┘  └──────────┘  └───────────┘              │
└──────────────────────────────────────────────────────────┘
```

## 核心数据流

### 下单流程

```
客户端 ──POST /api/v1/order/place──► Gateway (:8088)
                                         │
                                    JWT 验证
                                         │
                                    ┌────▼────┐
                                    │  Order  │ 路由到订单服务
                                    │  :8081  │
                                    └────┬────┘
                                         │
                         ┌───────────────┼───────────────┐
                         │               │               │
                    ┌────▼────┐    ┌─────▼─────┐   ┌────▼────┐
                    │  Risk   │    │  Matching │   │  Fund   │
                    │ 校验    │    │ 撮合      │   │ 冻结资金│
                    └─────────┘    └───────────┘   └─────────┘
                         │               │               │
                         └───────────────┼───────────────┘
                                    ┌────▼────┐
                                    │  Push   │ WebSocket 推送
                                    │  :8093  │ 成交/状态
                                    └─────────┘
```

### 行情数据流

```
行情源 ──► Market (:8086) ──► Redis (缓存最新价)
                │
                ├──► WebSocket Push (:8093) ──► 客户端
                │
                └──► Rest API (历史K线) ──► 客户端轮询
```

## 事件驱动架构

撮合引擎使用 **LMAX Disruptor** 实现无锁事件处理：

```
订单到达 ──► RingBuffer ──► EventHandler[0] (风控检查)
               │              EventHandler[1] (订单簿更新)
               │              EventHandler[2] (成交计算)
               │              EventHandler[3] (持久化)
               │              EventHandler[4] (推送通知)
               ▼
          Sequence Barrier
```

- RingBuffer 大小: 131,072
- 等待策略: BusySpin
- 批量处理: 每批最多 100 条事件

## 认证架构

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│  Client  │────►│  Gateway │────►│  Admin/Account│
│          │     │  JWT     │     │  (HMAC-SHA256)│
│ JWT Token│◄────│  Validate│◄────│  Generate     │
└──────────┘     └──────────┘     └──────────────┘
```

- Admin 服务签名: `HMAC-SHA256` (jjwt)
- 共享密钥: `futures-admin-secret-key-2024-rsa-encrypted`
- Order/Trade 服务复用相同的 `JwtTokenUtil` 验证
- 前端通过 `Authorization: Bearer <token>` 传递

## 数据库分库

| 服务 | 数据库 | 说明 |
|------|--------|------|
| futures-order | futures_order | 订单表、成交表 |
| futures-account | futures_account | 用户、KYC |
| futures-fund | futures_fund | 资金流水 |
| futures-risk | futures_risk | 风控配置、预警 |
| futures-market | futures_market | 行情快照 |
| futures-settlement | futures_settlement | 结算记录 |
| futures-admin | futures_admin | 运营后台表 |
| futures-push | futures_push | 推送记录 |
