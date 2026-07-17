CREATE TABLE IF NOT EXISTS t_risk_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    margin_rate DECIMAL(10,4),
    position_limit INT,
    warning_ratio DECIMAL(10,4),
    liquidation_ratio DECIMAL(10,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Entity @TableName("position_limit_config"), columns match PositionLimitConfigEntity
CREATE TABLE IF NOT EXISTS position_limit_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    symbol VARCHAR(20),
    max_position_volume INT DEFAULT 100,
    max_order_volume INT DEFAULT 100,
    enabled BOOLEAN DEFAULT TRUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_risk_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    alert_type VARCHAR(50),
    message VARCHAR(500),
    level VARCHAR(20),
    acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_forced_liquidation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20),
    quantity INT,
    price DECIMAL(20,2),
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (1, 'ES', 0.05, 100, 0.80, 1.20);
MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (2, 'GC', 0.08, 50, 0.80, 1.20);
MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (3, 'HSI', 0.06, 100, 0.80, 1.20);
MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (4, 'CL', 0.10, 50, 0.80, 1.20);
MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (5, 'EUR', 0.04, 200, 0.80, 1.20);
MERGE INTO t_risk_config (id, symbol, margin_rate, position_limit, warning_ratio, liquidation_ratio) KEY(symbol) VALUES (6, 'BTC', 0.15, 20, 0.80, 1.20);

-- Default position limit configs for ES and GC
MERGE INTO position_limit_config (id, user_id, symbol, max_position_volume, max_order_volume, enabled) KEY(id) VALUES (1, NULL, 'ES', 100, 50, TRUE);
MERGE INTO position_limit_config (id, user_id, symbol, max_position_volume, max_order_volume, enabled) KEY(id) VALUES (2, NULL, 'GC', 50, 25, TRUE);
