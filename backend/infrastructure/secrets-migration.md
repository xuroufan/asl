# 凭据迁移指南 — 从明文到 Secret 管理平台

## 当前状态

```
⚠ 待迁移的明文凭据:
├── futures-gateway-dev.yaml: JWT secret (hardcoded)
├── futures-account-dev.yaml: 数据库密码 (dev 环境)
├── application.yml: Redis 密码 fallback (${REDIS_PASSWORD:futures123})
└── .env / shell 脚本: 数据库密码明文
```

## 迁移路径

### 阶段 1: 环境变量化 (1-2 天)
```yaml
# ❌ 当前
password: futures123

# ✅ 目标
password: ${DB_PASSWORD}
```
只需将 application.yml / bootstrap.yml 中的明文替换为 `${VAR_NAME}`，
在 Docker Compose 或 K8s 中通过环境变量注入。

### 阶段 2: Docker Secrets (2-3 天)
```yaml
# docker-compose.yml
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  futures-account:
    secrets:
      - db_password
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
```

### 阶段 3: 专业 Secret 管理 (1-2 周)
根据部署环境选择以下方案之一：

#### 方案 A: HashiCorp Vault
```bash
# 安装 Vault
brew install vault
vault server -dev

# 存储凭据
vault kv put futures/db password=futures123

# Spring Boot 集成 (spring-cloud-starter-vault-config)
# bootstrap.yml:
spring.cloud.vault:
  host: 127.0.0.1
  port: 8200
  scheme: http
  kv:
    enabled: true
    backend: futures
```

#### 方案 B: AWS Secrets Manager
```bash
# 安装 AWS CLI
aws secretsmanager create-secret \
  --name futures/db-password \
  --secret-string '{"password":"futures123"}'

# Spring Boot 集成 (aws-secretsmanager-jdbc)
# application.yml:
aws:
  secretsmanager:
    prefix: /futures
    region: ap-northeast-1
```

#### 方案 C: 阿里云 KMS + Parameter Store
```bash
# 在 KMS 中创建密钥
aliyun kms CreateKey --Description="futures-db-key"

# 在 Parameter Store 中加密存储
aliyun oos CreateSecretParameter \
  --Name "futures-db-password" \
  --Value "futures123" \
  --Type "Secret"
```

## 检查清单

- [ ] 所有 application.yml 中无明文密码
- [ ] bootstrap.yml 配置 Secret 集成
- [ ] Docker Compose secrets 定义
- [ ] CI/CD 流水线注入环境变量
- [ ] 本地开发使用 .env.local（不提交到 Git）
- [ ] 定期轮换凭据（90 天）
