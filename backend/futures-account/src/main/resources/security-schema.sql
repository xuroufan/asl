-- 安全基础设施初始化（可选）
-- 此文件为 RBAC 权限模型的完整数据库定义。
-- 开发环境使用 UserEntity.trading_permissions 逗号分隔字段即可。

-- ============ 权限表 ============
CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码：order:create',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    description VARCHAR(255) COMMENT '权限描述',
    domain VARCHAR(50) NOT NULL COMMENT '所属领域：order/fund/position/admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============ 角色-权限关联表 ============
CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/VIP/ADMIN',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role, permission_code)
);

-- ============ 初始权限数据 ============
MERGE INTO t_permission (code, name, description, domain) KEY(code) VALUES
('order:create', '下单', '创建新订单', 'order'),
('order:cancel', '撤单', '撤销未成交订单', 'order'),
('order:query', '查询订单', '查询订单状态', 'order'),
('fund:view', '查看资金', '查看账户资金余额和流水', 'fund'),
('fund:withdraw', '出金', '提取账户资金', 'fund'),
('fund:deposit', '入金', '向账户充值', 'fund'),
('position:view', '查看持仓', '查看当前持仓', 'position'),
('position:close', '平仓', '平仓操作', 'position'),
('admin:permission:view', '查看权限', '查看用户权限配置', 'admin'),
('admin:permission:update', '修改权限', '修改用户权限配置', 'admin'),
('admin:role:update', '修改角色', '修改用户角色', 'admin'),
('admin:system:config', '系统配置', '修改系统级配置', 'admin');

-- ============ 默认角色权限分配 ============
MERGE INTO t_role_permission (role, permission_code) KEY(role, permission_code) VALUES
('USER', 'order:create'),
('USER', 'order:cancel'),
('USER', 'order:query'),
('USER', 'fund:view'),
('USER', 'position:view'),
('USER', 'position:close'),
('VIP', 'order:create'),
('VIP', 'order:cancel'),
('VIP', 'order:query'),
('VIP', 'fund:view'),
('VIP', 'fund:withdraw'),
('VIP', 'fund:deposit'),
('VIP', 'position:view'),
('VIP', 'position:close'),
('ADMIN', '*');
