CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 0,
    kyc_status INT DEFAULT 0 COMMENT 'KYC状态:0=未提交',
    real_name VARCHAR(50),
    id_card_no VARCHAR(20),
    id_card_front_url VARCHAR(500),
    id_card_back_url VARCHAR(500),
    trading_permissions VARCHAR(255) DEFAULT 'ALL',
    max_position_volume INT DEFAULT 1000,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(100),
    fail_count INT DEFAULT 0,
    lock_until TIMESTAMP,
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
    daily_loss_limit DECIMAL(20,2) DEFAULT 20000.00,
    total_pnl DECIMAL(20,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 测试用户 (密码: test123)
MERGE INTO t_user (id, username, password, display_name, role, status) KEY(username) VALUES (1, 'test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 'USER', 0);
MERGE INTO t_user (id, username, password, display_name, role, status) KEY(username) VALUES (2, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN', 0);

MERGE INTO t_account (id, user_id, cash_balance, equity_with_loan, available_funds) KEY(user_id) VALUES (1, 1, 1000000.00, 1000000.00, 1000000.00);
MERGE INTO t_account (id, user_id, cash_balance, equity_with_loan, available_funds) KEY(user_id) VALUES (2, 2, 5000000.00, 5000000.00, 5000000.00);