-- ========================================
-- 期货交易平台 - 数据库初始化脚本
-- ========================================

CREATE DATABASE IF NOT EXISTS futures_order DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS seata_config DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE futures_order;

-- ============ 用户表 ============
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    display_name VARCHAR(100) DEFAULT NULL COMMENT '显示名',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/VIP/ADMIN',
    status INT DEFAULT 0 COMMENT '状态：0正常 1锁定 2冻结',
    two_factor_enabled TINYINT(1) DEFAULT 0 COMMENT '是否开启2FA',
    two_factor_secret VARCHAR(100) DEFAULT NULL COMMENT '2FA密钥',
    fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_until DATETIME DEFAULT NULL COMMENT '锁定截止时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============ 账户表 ============
CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    cash_balance DECIMAL(20,2) DEFAULT 0.00 COMMENT '现金余额',
    equity_with_loan DECIMAL(20,2) DEFAULT 0.00 COMMENT '总权益',
    initial_margin DECIMAL(20,2) DEFAULT 0.00 COMMENT '占用保证金',
    maintenance_margin DECIMAL(20,2) DEFAULT 0.00 COMMENT '维持保证金',
    available_funds DECIMAL(20,2) DEFAULT 0.00 COMMENT '可用资金',
    daily_pnl DECIMAL(20,2) DEFAULT 0.00 COMMENT '日内盈亏',
    daily_loss_limit DECIMAL(20,2) DEFAULT 20000.00 COMMENT '日内亏损限额',
    total_pnl DECIMAL(20,2) DEFAULT 0.00 COMMENT '累计盈亏',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

-- ============ 订单表 ============
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    symbol VARCHAR(20) NOT NULL COMMENT '合约代码',
    side VARCHAR(10) NOT NULL COMMENT '方向：BUY/SELL',
    order_type VARCHAR(20) NOT NULL COMMENT '类型：MARKET/LIMIT/STOP/BRACKET',
    quantity INT NOT NULL COMMENT '委托数量',
    filled_quantity INT DEFAULT 0 COMMENT '已成交数量',
    limit_price DECIMAL(20,2) DEFAULT NULL COMMENT '限价',
    stop_price DECIMAL(20,2) DEFAULT NULL COMMENT '止损触发价',
    take_profit_price DECIMAL(20,2) DEFAULT NULL COMMENT '止盈价',
    parent_id BIGINT DEFAULT NULL COMMENT '父订单ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PARTIALLY_FILLED/FILLED/CANCELLED/REJECTED',
    avg_fill_price DECIMAL(20,2) DEFAULT NULL COMMENT '成交均价',
    time_in_force VARCHAR(10) DEFAULT 'DAY' COMMENT '有效期：DAY/IOC/GTC',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_symbol (symbol),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============ 持仓表 ============
CREATE TABLE IF NOT EXISTS t_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    symbol VARCHAR(20) NOT NULL COMMENT '合约代码',
    side VARCHAR(10) NOT NULL COMMENT '方向：LONG/SHORT',
    quantity INT NOT NULL COMMENT '持仓数量',
    avg_cost DECIMAL(20,2) NOT NULL COMMENT '开仓均价',
    current_price DECIMAL(20,2) DEFAULT NULL COMMENT '最新价',
    realized_pnl DECIMAL(20,2) DEFAULT 0.00 COMMENT '已实现盈亏',
    unrealized_pnl DECIMAL(20,2) DEFAULT 0.00 COMMENT '未实现盈亏',
    margin_used DECIMAL(20,2) DEFAULT 0.00 COMMENT '占用保证金',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_symbol (user_id, symbol),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓表';

-- ============ 初始数据 ============
-- 默认管理员/测试用户（密码：admin123）
INSERT INTO t_user (username, password, display_name, role, status) VALUES
('admin', 'admin123', '管理员', 'ADMIN', 0),
('test', 'test123', '测试用户', 'USER', 0);

-- 初始账户（初始资金100万）
INSERT INTO t_account (user_id, cash_balance, equity_with_loan, available_funds, daily_loss_limit) VALUES
(1, 1000000.00, 1000000.00, 1000000.00, 20000.00),
(2, 1000000.00, 1000000.00, 1000000.00, 20000.00);
