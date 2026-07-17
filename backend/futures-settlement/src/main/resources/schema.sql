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
    CONSTRAINT uk_user_date UNIQUE (user_id, settlement_date)
);

CREATE TABLE IF NOT EXISTS settlement_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    symbol VARCHAR(20),
    direction VARCHAR(10),
    open_price DECIMAL(20,4),
    close_price DECIMAL(20,4),
    volume INT DEFAULT 0,
    pnl DECIMAL(20,4),
    fee DECIMAL(20,4),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS margin_call (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_id BIGINT,
    margin_call_type VARCHAR(20) NOT NULL,
    required_amount DECIMAL(20,4) NOT NULL,
    current_margin DECIMAL(20,4),
    current_equity DECIMAL(20,4),
    status VARCHAR(20) DEFAULT 'PENDING',
    sent_time TIMESTAMP,
    resolved_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reconciliation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciliation_date DATE NOT NULL,
    reconciliation_type VARCHAR(20) NOT NULL,
    total_records INT DEFAULT 0,
    matched_records INT DEFAULT 0,
    unmatched_records INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    summary VARCHAR(1000),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reconciliation_diff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciliation_id BIGINT NOT NULL,
    diff_type VARCHAR(20) NOT NULL,
    our_record_id VARCHAR(64),
    their_record_id VARCHAR(64),
    our_amount DECIMAL(20,4),
    their_amount DECIMAL(20,4),
    amount_diff DECIMAL(20,4),
    status VARCHAR(20) DEFAULT 'PENDING',
    notes VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS regulatory_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE NOT NULL,
    report_type VARCHAR(20) NOT NULL,
    format VARCHAR(10) NOT NULL,
    file_path VARCHAR(500),
    status VARCHAR(20) DEFAULT 'GENERATING',
    summary VARCHAR(2000),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
