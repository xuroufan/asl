# Nacos 配置中心使用指南

## 概述

本配置中心为期货交易平台 9 个微服务提供统一的配置管理能力，基于 Alibaba Nacos 2.3.2 实现。

### 核心能力

| 能力 | 说明 |
|------|------|
| 配置集中管理 | 所有服务配置统一存储在 Nacos，支持分组隔离 |
| 热更新 | 配置变更后服务秒级生效，无需重启 (@RefreshScope) |
| 多环境隔离 | dev / test / prod 通过 Namespace 隔离 |
| 版本管理 | 每次修改自动生成版本号，支持一键回滚 |
| 灰度发布 | 支持按服务实例权重分配流量 |
| 配置加密 | 敏感字段（密码、密钥）支持 AES-256 加密 |

---

## 目录结构

```
nacos-config/
├── config-center-guide.md          # 本文档
├── bootstrap-template.yaml         # bootstrap.yml 模板
├── global/
│   ├── futures-shared-dev.yaml     # 全局共享配置 - 开发(MySQL版)
│   ├── futures-shared-dev-h2.yaml  # 全局共享配置 - 开发(H2版)
│   └── futures-shared-prod.yaml    # 全局共享配置 - 生产
├── services/
│   ├── futures-gateway-dev.yaml    # 网关配置
│   ├── futures-order-dev.yaml      # 订单服务配置
│   ├── futures-matching-dev.yaml   # 撮合引擎配置
│   ├── futures-account-dev.yaml    # 账户服务配置
│   ├── futures-fund-dev.yaml       # 资金服务配置
│   ├── futures-risk-dev.yaml       # 风控服务配置
│   ├── futures-market-dev.yaml     # 行情服务配置
│   ├── futures-settlement-dev.yaml # 清结算服务配置
│   └── *.prod.yaml                 # 生产环境对应版本
├── scripts/
│   └── import-configs.sh           # 配置批量导入脚本
├── demo-code/
│   ├── DynamicConfigController.java  # @RefreshScope 示例
│   ├── NacosConfigListener.java      # 配置变更监听示例
│   └── ConfigCipher.java             # AES 加密工具
└── k8s/ (optional)                 # K8s 部署模板
```

---

## 配置层级与优先级

```
环境特定配置 (futures-{service}-{profile}.yaml)     ← 最高优先级
      ↓
服务级配置 (futures-{service}.yaml)                   ← 中等优先级
      ↓
全局共享配置 (futures-shared-{profile}.yaml)          ← 最低优先级
```

### bootstrap.yml 加载顺序

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:          # 1. 全局配置 (优先级最低)
          - data-id: futures-shared-dev-h2.yaml
            group: GLOBAL_GROUP
        extension-configs:       # 2. 服务级默认值
          - data-id: futures-order.yaml
            group: SERVICE_GROUP
        # 3. 主配置 (优先级最高)
        #    Data ID = ${spring.application.name}-${spring.profiles.active}.yaml
```

---

## 快速启动 (开发环境)

### 前置条件

- Docker Desktop 已安装并运行
- Docker Compose v2+

### 第1步：启动 Nacos 及相关中间件

```bash
cd futures-platform

# 只启动 Nacos 所需的依赖
docker compose up -d nacos mysql

# 验证 Nacos 是否启动成功
docker logs futures-nacos --tail 20

# 访问 Nacos 控制台
open http://localhost:8848/nacos
# 默认账号: nacos / nacos
```

### 第2步：导入配置到 Nacos

```bash
# 导入所有 dev 环境的配置
cd infrastructure/nacos-config
chmod +x scripts/import-configs.sh

# 导入 H2 版全局配置 + 所有服务配置
./scripts/import-configs.sh dev
```

### 第3步：更新服务 bootstrap.yml

更新每个服务的 `src/main/resources/bootstrap.yml`，将 Nacos 启用：

```yaml
# 开发模式下启用 Nacos 配置中心
spring:
  application:
    name: futures-order              # 服务名需与 Nacos 配置的 Data ID 前缀一致
  cloud:
    nacos:
      config:
        enabled: true                # ← 从 false 改为 true
        server-addr: localhost:8848
        namespace: futures-platform
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true
        shared-configs:
          - data-id: futures-shared-dev-h2.yaml   # 使用 H2 版共享配置
            group: GLOBAL_GROUP
            refresh: true
      discovery:
        enabled: true                # ← 从 false 改为 true
        server-addr: localhost:8848
        namespace: futures-platform
```

### 第4步：重启服务并验证

```bash
# 停止旧服务 (Ctrl+C 或 kill)
screen -S fm-order -X quit

# 重新启动 (使用新的 bootstrap.yml)
cd futures-order
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments="-Dspring.classformat.ignore=true"
```

**验证方法：**

1. **Nacos 控制台** → "服务管理" → 检查服务是否注册成功
2. **Nacos 控制台** → "配置管理" → 检查配置是否被引用
3. **应用日志**：搜索 `NacosConfigService` 相关日志
4. **API 测试**：调用 `GET /api/v1/order/config` 查看动态配置

---

## 配置热更新

### 1. 使用 @RefreshScope

在需要动态刷新的 Bean 上添加注解：

```java
@RestController
@RequestMapping("/api/v1/demo")
@RefreshScope   // ← 关键注解
public class DynamicConfigController {
    @Value("${futures.order.max-volume-per-order:999}")
    private int maxVolumePerOrder;
}
```

配置变更后，调用 POST 刷新端点即可立即生效。

### 2. 监听配置变更事件

使用 `@NacosConfigListener` 监听特定 Data ID 的变化，适合需要执行额外逻辑的场景（如：重新初始化连接池）。

### 3. 验证热更新

```bash
# 1. 访问热更新端点，查看当前配置
curl http://localhost:8081/api/v1/demo/config

# 2. 在 Nacos 控制台修改配置值

# 3. 触发配置刷新
curl -X POST http://localhost:8081/api/v1/demo/refresh

# 4. 再次查看，确认配置已更新
curl http://localhost:8081/api/v1/demo/config
```

---

## 配置加密

### 加密工具使用

```bash
# 编译并运行 ConfigCipher.java
cd infrastructure/nacos-config/demo-code
javac ConfigCipher.java

# 加密敏感信息
java ConfigCipher encrypt "futures123"
# 输出: ENC(GH5p3kP+...)

# 设置解密密钥环境变量
export CONFIG_DECRYPT_KEY=your-secret-key-32bytes

# 在配置中使用
spring:
  datasource:
    password: ENC(GH5p3kP+...)    # 启动时自动解密
```

### 加密注意事项

- 解密密钥通过环境变量注入，不写入任何配置文件
- 加密后的值以 `ENC()` 格式标识
- 生产环境建议使用专门的密钥管理服务

---

## 多环境管理

### Namespace 规划

| Namespace | 用途 | ID |
|-----------|------|----|
| `futures-platform-dev` | 开发环境 | 自动生成 |
| `futures-platform-test` | 测试环境 | 自动生成 |
| `futures-platform-prod` | 生产环境 | 自动生成 |

### 配置切换

```bash
# 开发环境（默认）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 测试环境
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 生产环境
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 配置一览

### 全局配置 (Data ID: futures-shared-dev-h2.yaml)

```
Group: GLOBAL_GROUP | 适用于 H2 内存数据库开发模式

spring.datasource          → H2 连接配置
spring.data.redis          → Redis 单机版配置
mybatis-plus.*             → MyBatis-Plus 全局设置
```

### 服务配置 (示例: futures-order-dev.yaml)

```
Group: DEFAULT_GROUP

server.port                    → 8081
futures.order.max-volume-per-order → 999 (单笔最大手数)
futures.order.order-timeout-seconds → 30 (订单超时秒数)
futures.order.supported-order-types  → MARKET/LIMIT/STOP 等
```

| 配置文件名 | 服务 | 端口 | 关键配置项 |
|-----------|------|------|-----------|
| `futures-gateway-dev.yaml` | API网关 | 8088 | 路由规则、JWT参数、Sentinel限流 |
| `futures-order-dev.yaml` | 订单服务 | 8081 | 下单限额、超时、支持订单类型 |
| `futures-matching-dev.yaml` | 撮合引擎 | 8082 | Disruptor参数、快照策略 |
| `futures-account-dev.yaml` | 账户服务 | 8083 | KYC配置、Token过期、登录锁定 |
| `futures-fund-dev.yaml` | 资金服务 | 8084 | 初始资金、杠杆、出入金额度 |
| `futures-risk-dev.yaml` | 风控服务 | 8085 | 保证金率、强平阈值 |
| `futures-market-dev.yaml` | 行情服务 | 8086 | 模拟器参数、K线周期 |
| `futures-settlement-dev.yaml` | 清结算 | 8087 | 结算时间、手续费率 |

---

## 常见问题

### Q: 启动时报 "Nacos server is not connected"

**原因**：Nacos 服务未启动或地址配置错误。

**解决**：
```bash
# 检查 Nacos 是否运行
docker ps | grep nacos

# 确认地址正确
# 开发环境(Docker内)使用: nacos:8848
# 开发环境(本地)使用: localhost:8848
# 生产环境使用: nacos.futures-platform.com:8848
```

### Q: 配置变更后未生效

**原因**：
1. Bean 未添加 `@RefreshScope` 注解
2. `refresh-enabled` 未设置为 `true`

**解决**：参照 `DynamicConfigController.java` 的写法。

### Q: 导入配置失败

**原因**：Nacos API 版本不匹配或 Token 过期。

**解决**：
```bash
# 检查网络连通性
curl http://localhost:8848/nacos/v1/auth/login \
  -X POST -d "username=nacos&password=nacos"

# 手动导入单个配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=futures-order-dev.yaml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=$(cat services/futures-order-dev.yaml)"
```

---

## 生产环境注意事项

1. **MySQL 替代 H2**：生产环境使用 `futures-shared-prod.yaml`（MySQL 版）
2. **Nacos 集群**：生产环境部署 3 节点 Nacos 集群（参考 `nacos-cluster.md`）
3. **配置加密**：生产环境的数据库密码等敏感信息必须加密
4. **权限控制**：使用 Nacos 的 RBAC 权限管理
5. **监控告警**：配置变更审计、Nacos 服务健康监控
6. **备份策略**：定期备份 Nacos 配置数据（MySQL 备份）

---

## 相关文档

- [Nacos 官方文档](https://nacos.io/docs/latest/overview/)
- [Spring Cloud Alibaba 配置中心](https://sca.aliyun.com/zh-cn/docs/next/user-guide/nacos/overview)
- [基础设施架构文档](../architecture.md)
- [版本兼容性矩阵](../version-matrix.md)
- [Nacos 集群部署指南](../nacos-cluster.md)
