-- ============================================================
-- Futures Trading Platform - Database Schema Initialization
-- 创建所有微服务所需的数据库和表
-- 同时创建复制用户
-- ============================================================

-- 创建各服务数据库
CREATE DATABASE IF NOT EXISTS futures_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_account CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_fund CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_matching CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_risk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_settlement CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS futures_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 创建复制用户（主从同步专用）
-- ============================================================
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'futures_repl_2024';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- ============================================================
-- 订单服务 - futures_order
-- ============================================================
USE futures_order;

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY,
    order_id VARCHAR(32) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    direction INT NOT NULL COMMENT '0:BUY, 1:SELL',
    order_type INT NOT NULL COMMENT '0:LIMIT, 1:MARKET, 2:STOP, 3:STOP_LIMIT',
    price DECIMAL(20,4),
    volume INT NOT NULL,
    filled_volume INT DEFAULT 0,
    avg_price DECIMAL(20,4),
    stop_price DECIMAL(20,4),
    take_profit_price DECIMAL(20,4),
    parent_id BIGINT,
    status INT DEFAULT 0 COMMENT '0:PENDING, 1:PARTIAL, 2:FILLED, 3:CANCELLED, 4:REJECTED',
    client_order_id VARCHAR(64),
    time_in_force VARCHAR(10) DEFAULT 'DAY',
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_symbol (user_id, symbol),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
);

CREATE TABLE IF NOT EXISTS t_fill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    direction INT NOT NULL,
    fill_price DECIMAL(20,4) NOT NULL,
    fill_volume INT NOT NULL,
    fill_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_user (user_id)
);

-- ============================================================
-- 账户服务 - futures_account
-- ============================================================
USE futures_account;

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 0 COMMENT '0:正常, 1:冻结, 2:销户',
    kyc_status INT DEFAULT 0 COMMENT '0:未提交, 1:审核中, 2:已通过, 3:已拒绝',
    real_name VARCHAR(50),
    id_card_no VARCHAR(20),
    id_card_front_url VARCHAR(500),
    id_card_back_url VARCHAR(500),
    trading_permissions VARCHAR(255) DEFAULT 'ALL',
    max_position_volume INT DEFAULT 1000,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(100),
    fail_count INT DEFAULT 0,
    lock_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    cash_balance DECIMAL(20,2) DEFAULT 0.00,
    equity_with_loan DECIMAL(20,2) DEFAULT 0.00,
    initial_margin DECIMAL(20,2) DEFAULT 0.00,
    maintenance_margin DECIMAL(20,2) DEFAULT 0.00,
    available_funds DECIMAL(20,2) DEFAULT 0.00,
    daily_pnl DECIMAL(20,2) DEFAULT 0.00,
    daily_loss_limit DECIMAL(20,2) DEFAULT 2000.00,
    risk_ratio DECIMAL(10,2) DEFAULT 0.00,
    margin_call BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role, permission_code)
);

CREATE TABLE IF NOT EXISTS t_user_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_perm (user_id, permission_code)
);

-- ============================================================
-- 资金服务 - futures_fund
-- ============================================================
USE futures_fund;

CREATE TABLE IF NOT EXISTS kyc_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    real_name VARCHAR(50),
    id_card_no VARCHAR(20),
    id_card_front_url VARCHAR(500),
    id_card_back_url VARCHAR(500),
    status INT DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已拒绝',
    remark VARCHAR(200),
    reviewer VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_user (user_id)
);


CREATE TABLE IF NOT EXISTS t_fund_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    version INT DEFAULT 0,
    float_profit DECIMAL(20,2) DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'HKD',
    status INT DEFAULT 0,
    balance DECIMAL(20,2) DEFAULT 0.00,
    frozen DECIMAL(20,2) DEFAULT 0.00,
    margin DECIMAL(20,2) DEFAULT 0.00,
    available DECIMAL(20,2) DEFAULT 0.00,
    daily_pnl DECIMAL(20,2) DEFAULT 0.00,
    daily_loss_limit DECIMAL(20,2) DEFAULT 20000.00,
    equity_with_loan DECIMAL(20,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_fund_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flow_type INT COMMENT '0=入金 1=出金 2=冻结 3=解冻 4=扣款 5=入账',
    amount DECIMAL(20,2),
    before_balance DECIMAL(20,2),
    after_balance DECIMAL(20,2),
    before_available DECIMAL(20,2) DEFAULT 0.00,
    after_available DECIMAL(20,2) DEFAULT 0.00,
    before_frozen DECIMAL(20,2) DEFAULT 0.00,
    after_frozen DECIMAL(20,2) DEFAULT 0.00,
    order_id VARCHAR(32),
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_time (created_at)
);

CREATE TABLE IF NOT EXISTS deposit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING,SUCCESS,FAILED',
    channel VARCHAR(50),
    channel_order_id VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS withdraw_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    bank_info VARCHAR(200),
    status INT DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已拒绝 3=已完成',
    reviewer VARCHAR(50),
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    review_time DATETIME,
    deleted INT DEFAULT 0
);

-- ============================================================
-- 撮合引擎 - futures_matching  
-- ============================================================
USE futures_matching;

CREATE TABLE IF NOT EXISTS order_book_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    snapshot_data JSON,
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_symbol_time (symbol, snapshot_time)
);

CREATE TABLE IF NOT EXISTS matching_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL,
    event_data JSON,
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_time (event_type, event_time)
);

-- ============================================================
-- 风控引擎 - futures_risk
-- ============================================================
USE futures_risk;

CREATE TABLE IF NOT EXISTS t_risk_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    margin_rate DECIMAL(10,4) DEFAULT 0.05,
    position_limit INT DEFAULT 500,
    warning_ratio DECIMAL(10,4) DEFAULT 0.80,
    liquidation_ratio DECIMAL(10,4) DEFAULT 1.20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_symbol (symbol)
);

CREATE TABLE IF NOT EXISTS position_limit_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    symbol VARCHAR(20),
    max_position_volume INT DEFAULT 100,
    max_order_volume INT DEFAULT 100,
    enabled BOOLEAN DEFAULT TRUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_user_symbol (user_id, symbol)
);

CREATE TABLE IF NOT EXISTS t_liquidation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20),
    direction VARCHAR(10),
    volume INT,
    liquidation_price DECIMAL(20,2),
    risk_ratio DECIMAL(10,4),
    reason VARCHAR(200),
    status INT DEFAULT 0,
    order_id VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
);


CREATE TABLE IF NOT EXISTS t_risk_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    alert_type VARCHAR(50),
    message VARCHAR(500),
    level VARCHAR(20) COMMENT 'INFO,WARN,CRITICAL',
    acknowledged BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_level_time (level, create_time)
);

CREATE TABLE IF NOT EXISTS forced_liquidation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20),
    volume INT,
    price DECIMAL(20,4),
    reason VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
);

-- ============================================================
-- 行情服务 - futures_market
-- ============================================================
USE futures_market;

CREATE TABLE IF NOT EXISTS t_market_symbol (
    id INT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    exchange VARCHAR(20),
    contract_size INT DEFAULT 1,
    tick_size DECIMAL(10,4) DEFAULT 0.01,
    currency VARCHAR(10) DEFAULT 'HKD',
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_kline_1m (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open_time BIGINT NOT NULL,
    open_price DECIMAL(20,4),
    high_price DECIMAL(20,4),
    low_price DECIMAL(20,4),
    close_price DECIMAL(20,4),
    volume BIGINT DEFAULT 0,
    UNIQUE KEY uk_symbol_time (symbol, open_time)
);

CREATE TABLE IF NOT EXISTS t_kline_5m (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open_time BIGINT NOT NULL,
    open_price DECIMAL(20,4),
    high_price DECIMAL(20,4),
    low_price DECIMAL(20,4),
    close_price DECIMAL(20,4),
    volume BIGINT DEFAULT 0,
    UNIQUE KEY uk_symbol_time (symbol, open_time)
);

CREATE TABLE IF NOT EXISTS t_kline_1d (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    open_time BIGINT NOT NULL,
    open_price DECIMAL(20,4),
    high_price DECIMAL(20,4),
    low_price DECIMAL(20,4),
    close_price DECIMAL(20,4),
    volume BIGINT DEFAULT 0,
    UNIQUE KEY uk_symbol_time (symbol, open_time)
);

-- ============================================================
-- 清结算服务 - futures_settlement
-- ============================================================
USE futures_settlement;

CREATE TABLE IF NOT EXISTS daily_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    begin_equity DECIMAL(20,4),
    end_equity DECIMAL(20,4),
    realized_pnl DECIMAL(20,4),
    unrealized_pnl DECIMAL(20,4),
    total_pnl DECIMAL(20,4),
    fee DECIMAL(20,4),
    net_deposit DECIMAL(20,4),
    opening_margin DECIMAL(20,4),
    closing_margin DECIMAL(20,4),
    maintenance_margin DECIMAL(20,4),
    margin_call_amount DECIMAL(20,4),
    settlement_price DECIMAL(20,4),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settled_time TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    CONSTRAINT uk_user_date UNIQUE (user_id, settlement_date),
    INDEX idx_user (user_id),
    INDEX idx_date (settlement_date)
);

CREATE TABLE IF NOT EXISTS settlement_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(32),
    symbol VARCHAR(20),
    direction VARCHAR(10),
    volume INT,
    price DECIMAL(20,4),
    pnl DECIMAL(20,4),
    fee DECIMAL(20,4),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_settlement (settlement_id)
);

-- ============================================================
-- 默认种子数据
-- ============================================================

-- 风控默认配置
USE futures_risk;
INSERT IGNORE INTO t_risk_config (symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) VALUES
('ES', 0.05, 500, 0.80, 1.20),
('GC', 0.10, 200, 0.80, 1.20),
('NQ', 0.08, 300, 0.80, 1.20),
('CL', 0.12, 300, 0.80, 1.20),
('6E', 0.05, 400, 0.80, 1.20);

CREATE TABLE IF NOT EXISTS reconciliation_diff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    diff_type VARCHAR(50),
    diff_amount DECIMAL(20,4),
    detail VARCHAR(500),
    status INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, settlement_date)
);

CREATE TABLE IF NOT EXISTS margin_call (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    margin_call_amount DECIMAL(20,4),
    margin_ratio DECIMAL(10,4),
    call_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    filled_time DATETIME,
    status INT DEFAULT 0 COMMENT '0=未处理 1=已追加 2=已强平',
    INDEX idx_user (user_id)
);

CREATE TABLE IF NOT EXISTS reconciliation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(50) NOT NULL,
    reconciliation_date DATE NOT NULL,
    total_count INT DEFAULT 0,
    matched_count INT DEFAULT 0,
    diff_count INT DEFAULT 0,
    status INT DEFAULT 0 COMMENT '0=进行中 1=完成 2=异常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME
);

CREATE TABLE IF NOT EXISTS regulatory_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    report_date DATE NOT NULL,
    content JSON,
    status INT DEFAULT 0 COMMENT '0=待提交 1=已提交',
    submitted_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- 行情默认品种
USE futures_market;
INSERT IGNORE INTO t_market_symbol (symbol, name, exchange, contract_size, tick_size, currency) VALUES
('ES', 'E-mini S&P 500', 'CME', 50, 0.25, 'USD'),
('NQ', 'E-mini Nasdaq 100', 'CME', 20, 0.25, 'USD'),
('GC', 'Gold Futures', 'COMEX', 100, 0.10, 'USD'),
('CL', 'Crude Oil Futures', 'NYMEX', 1000, 0.01, 'USD'),
('6E', 'Euro FX Futures', 'CME', 125000, 0.00005, 'USD');

-- 默认权限数据
USE futures_account;
INSERT IGNORE INTO t_permission (code, name, description) VALUES
('order:create', '创建订单', '创建交易订单'),
('order:cancel', '撤销订单', '撤销未成交订单'),
('order:view', '查看订单', '查看订单列表和详情'),
('position:view', '查看持仓', '查看持仓信息'),
('position:trade', '交易持仓', '平仓操作'),
('account:view', '查看账户', '查看账户信息'),
('fund:view', '查看资金', '查看资金信息'),
('fund:transfer', '资金转账', '出入金操作'),
('market:view', '查看行情', '查看行情数据'),
('admin:user', '用户管理', '管理用户'),
('admin:risk', '风控管理', '管理风控参数'),
('admin:permission', '权限管理', '管理用户权限');

INSERT IGNORE INTO t_role_permission (role, permission_code) VALUES
('ADMIN', 'order:create'), ('ADMIN', 'order:cancel'), ('ADMIN', 'order:view'),
('ADMIN', 'position:view'), ('ADMIN', 'position:trade'),
('ADMIN', 'account:view'), ('ADMIN', 'fund:view'), ('ADMIN', 'fund:transfer'),
('ADMIN', 'market:view'),
('ADMIN', 'admin:user'), ('ADMIN', 'admin:risk'), ('ADMIN', 'admin:permission'),
('USER', 'order:create'), ('USER', 'order:cancel'), ('USER', 'order:view'),
('USER', 'position:view'), ('USER', 'position:trade'),
('USER', 'account:view'), ('USER', 'fund:view'), ('USER', 'fund:transfer'),
('USER', 'market:view');

-- Seata 分布式事务表
-- (如果不存在则会创建，由 Seata 自行管理)

SELECT '✅ 所有数据库和表创建完成' AS status;
