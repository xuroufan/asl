CREATE TABLE IF NOT EXISTS fund_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    version INT DEFAULT 0,
    float_pnl DECIMAL(20,2) DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'HKD',
    status INT DEFAULT 0,
    cash_balance DECIMAL(20,2) DEFAULT 0.00,
    frozen_margin DECIMAL(20,2) DEFAULT 0.00,
    used_margin DECIMAL(20,2) DEFAULT 0.00,
    available_funds DECIMAL(20,2) DEFAULT 0.00,
    daily_pnl DECIMAL(20,2) DEFAULT 0.00,
    daily_loss_limit DECIMAL(20,2) DEFAULT 20000.00,
    equity_with_loan DECIMAL(20,2) DEFAULT 0.00,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fund_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flow_type VARCHAR(20),
    amount DECIMAL(20,2),
    balance_before DECIMAL(20,2),
    balance_after DECIMAL(20,2),
    order_id BIGINT,
    symbol VARCHAR(20),
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS withdraw_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(20,2),
    bank_info VARCHAR(100),
    status INT DEFAULT 0,
    reviewer VARCHAR(50),
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    review_time DATETIME,
    deleted INT DEFAULT 0
);

-- 初始资金
MERGE INTO fund_account (id, user_id, cash_balance, available_funds, equity_with_loan, currency)
    KEY(user_id) VALUES (1, 1, 1000000.00, 1000000.00, 1000000.00, 'HKD');
MERGE INTO fund_account (id, user_id, cash_balance, available_funds, equity_with_loan, currency)
    KEY(user_id) VALUES (2, 2, 5000000.00, 5000000.00, 5000000.00, 'HKD');
