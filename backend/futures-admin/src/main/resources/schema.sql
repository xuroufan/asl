-- 创建 admin 数据库（首次执行）
CREATE DATABASE IF NOT EXISTS futures_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE futures_admin;

-- 1. 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    dept_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    ancestors VARCHAR(500) DEFAULT '' COMMENT '祖级列表',
    dept_name VARCHAR(30) NOT NULL COMMENT '部门名称',
    order_num INT DEFAULT 0 COMMENT '显示顺序',
    leader VARCHAR(20) DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='部门表';

-- 2. 岗位表
CREATE TABLE IF NOT EXISTS sys_post (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_code VARCHAR(64) NOT NULL COMMENT '岗位编码',
    post_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
    post_sort INT NOT NULL COMMENT '显示顺序',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='岗位表';

-- 3. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    post_id BIGINT DEFAULT NULL COMMENT '岗位ID',
    username VARCHAR(30) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(30) DEFAULT '' COMMENT '昵称',
    phone VARCHAR(11) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像',
    sex TINYINT DEFAULT 0 COMMENT '0未知 1男 2女',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    login_ip VARCHAR(50) DEFAULT NULL,
    login_date DATETIME DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='用户表';

-- 4. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(100) NOT NULL UNIQUE COMMENT '角色权限字符串',
    role_sort INT DEFAULT 0 COMMENT '显示顺序',
    data_scope TINYINT DEFAULT 1 COMMENT '1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='角色表';

-- 5. 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type TINYINT DEFAULT 0 COMMENT '0目录 1菜单 2按钮',
    path VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    perms VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    icon VARCHAR(100) DEFAULT '#' COMMENT '图标',
    order_num INT DEFAULT 0 COMMENT '显示顺序',
    visible TINYINT DEFAULT 0 COMMENT '0显示 1隐藏',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='菜单表';

-- 6. 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关联表';

-- 7. 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) COMMENT='角色菜单关联表';

-- 8. 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    dict_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_name VARCHAR(100) DEFAULT '' COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL UNIQUE COMMENT '字典类型',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='字典类型表';

-- 9. 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    dict_code BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_sort INT DEFAULT 0 COMMENT '字典排序',
    dict_label VARCHAR(100) DEFAULT '' COMMENT '字典标签',
    dict_value VARCHAR(100) DEFAULT '' COMMENT '字典键值',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    css_class VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    status TINYINT DEFAULT 0 COMMENT '0正常 1停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='字典数据表';

-- 10. 操作日志表
CREATE TABLE IF NOT EXISTS sys_oper_log (
    oper_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) DEFAULT '' COMMENT '模块标题',
    oper_type INT DEFAULT 0 COMMENT '操作类型',
    method VARCHAR(100) DEFAULT '' COMMENT '方法名称',
    request_method VARCHAR(10) DEFAULT '' COMMENT '请求方式',
    oper_name VARCHAR(50) DEFAULT '' COMMENT '操作人员',
    dept_name VARCHAR(50) DEFAULT '' COMMENT '部门名称',
    oper_url VARCHAR(255) DEFAULT '' COMMENT '请求URL',
    oper_ip VARCHAR(50) DEFAULT '' COMMENT '主机地址',
    oper_location VARCHAR(255) DEFAULT '' COMMENT '操作地点',
    oper_param TEXT COMMENT '请求参数',
    json_result TEXT COMMENT '返回参数',
    status INT DEFAULT 0 COMMENT '0正常 1异常',
    cost_time BIGINT DEFAULT 0 COMMENT '消耗时间(ms)',
    error_msg VARCHAR(2000) DEFAULT '' COMMENT '错误消息',
    oper_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='操作日志表';

-- 11. 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    info_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) DEFAULT '' COMMENT '用户名',
    status TINYINT DEFAULT 0 COMMENT '0成功 1失败',
    ip VARCHAR(50) DEFAULT '' COMMENT 'IP地址',
    location VARCHAR(255) DEFAULT '' COMMENT '登录地点',
    msg VARCHAR(255) DEFAULT '' COMMENT '提示消息',
    browser VARCHAR(50) DEFAULT '' COMMENT '浏览器',
    os VARCHAR(50) DEFAULT '' COMMENT '操作系统',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='登录日志表';

-- 12. 参数配置表
CREATE TABLE IF NOT EXISTS sys_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) DEFAULT '' COMMENT '参数名称',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '参数键名',
    config_value VARCHAR(500) DEFAULT '' COMMENT '参数键值',
    config_type TINYINT DEFAULT 0 COMMENT '0系统内置 1自定义',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='参数配置表';

-- ============ 初始数据 ============
-- 密码均为 admin123 (BCrypt)
INSERT INTO sys_user (user_id, username, password, nickname, dept_id, status) VALUES
(1, 'admin', '$2a$10$IZ7otMBh67E.1SzSGWjPa.xt8.UXrhOQFabmLQtcLTKu3u6Jpcxxm', '管理员', 100, 0),
(2, 'dev', '$2a$10$IZ7otMBh67E.1SzSGWjPa.xt8.UXrhOQFabmLQtcLTKu3u6Jpcxxm', '运维人员', 101, 0),
(3, 'audit', '$2a$10$IZ7otMBh67E.1SzSGWjPa.xt8.UXrhOQFabmLQtcLTKu3u6Jpcxxm', '审计人员', 101, 0);

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope) VALUES
(1, '超级管理员', 'admin', 1, 1),
(2, '运维角色', 'ops', 2, 3),
(3, '审计角色', 'audit', 3, 5);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num) VALUES
(100, 0, '0', '期货交易平台', 0),
(101, 100, '0,100', '技术部', 1),
(102, 100, '0,100', '风控部', 2),
(103, 100, '0,100', '运营部', 3);

-- 初始菜单（管理后台左侧导航）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num) VALUES
(1, 0, '系统管理', 0, '/system', NULL, NULL, 'Setting', 1),
(2, 1, '用户管理', 1, '/system/user', '/system/user/index', 'system:user:list', 'User', 1),
(3, 1, '角色管理', 1, '/system/role', '/system/role/index', 'system:role:list', 'Avatar', 2),
(4, 1, '菜单管理', 1, '/system/menu', '/system/menu/index', 'system:menu:list', 'Menu', 3),
(5, 1, '部门管理', 1, '/system/dept', '/system/dept/index', 'system:dept:list', 'OfficeBuilding', 4),
(6, 1, '字典管理', 1, '/system/dict', '/system/dict/index', 'system:dict:list', 'Notebook', 5),
(7, 1, '参数设置', 1, '/system/config', '/system/config/index', 'system:config:list', 'Coin', 6),

(8, 0, '系统监控', 0, '/monitor', NULL, NULL, 'Monitor', 2),
(9, 8, '在线用户', 1, '/monitor/online', '/monitor/online/index', 'monitor:online:list', 'Link', 1),
(10, 8, '操作日志', 1, '/monitor/operlog', '/monitor/operlog/index', 'monitor:operlog:list', 'Document', 2),
(11, 8, '登录日志', 1, '/monitor/loginlog', '/monitor/loginlog/index', 'monitor:loginlog:list', 'Lock', 3);

-- 初始字典
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type) VALUES
(1, '系统状态', 'sys_status'),
(2, '交易品种', 'trade_symbol'),
(3, '订单状态', 'order_status'),
(4, '风控等级', 'risk_level');

INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_type, dict_sort, css_class) VALUES
(1, '正常', '0', 'sys_status', 1, 'primary'),
(2, '停用', '1', 'sys_status', 2, 'danger'),
(3, 'HSI', 'HSI', 'trade_symbol', 1, ''),
(4, 'ES', 'ES', 'trade_symbol', 2, ''),
(5, 'GC', 'GC', 'trade_symbol', 3, ''),
(6, 'CL', 'CL', 'trade_symbol', 4, ''),
(7, '已报', 'PENDING', 'order_status', 1, 'primary'),
(8, '部成', 'PARTIAL', 'order_status', 2, 'warning'),
(9, '全成', 'FILLED', 'order_status', 3, 'success'),
(10, '已撤', 'CANCELLED', 'order_status', 4, 'info'),
(11, '废单', 'REJECTED', 'order_status', 5, 'danger'),
(12, '正常', '0', 'risk_level', 1, 'success'),
(13, '预警', '1', 'risk_level', 2, 'warning'),
(14, '强平', '2', 'risk_level', 3, 'danger');

-- 初始参数配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark) VALUES
('交易时段-早盘', 'trade.session.morning', '09:00-12:00', 1, '上午交易时段'),
('交易时段-午盘', 'trade.session.afternoon', '13:00-16:00', 1, '下午交易时段'),
('默认保证金率', 'trade.default.margin.rate', '0.1', 1, '默认保证金率10%'),
('默认手续费率', 'trade.default.fee.rate', '0.0003', 1, '默认手续费率万分之三'),
('单笔最大手数', 'trade.max.volume', '9999', 1, '单笔下单最大手数限制');

-- 13. 风控管理菜单（menu_id 12~16）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num) VALUES
(12, 0, '风控管理', 0, '/risk', NULL, NULL, 'Warning', 3),
(13, 12, '风控规则配置', 1, '/risk/config', '/risk/config/index', 'risk:config:list', 'Setting', 1),
(14, 12, '风控监控大屏', 1, '/risk/dashboard', '/risk/dashboard/index', 'risk:monitor:list', 'Monitor', 2),
(15, 12, '强平记录', 1, '/risk/liquidation', '/risk/liquidation/index', 'risk:liquidation:list', 'Warning', 3),
(16, 12, '异常报警', 1, '/risk/alert', '/risk/alert/index', 'risk:alert:list', 'AlarmClock', 4),
(17, 12, '风控报表', 1, '/risk/report', '/risk/report/index', 'risk:report:list', 'DataLine', 5);

-- 给超级管理员（role_id=1）授予风控菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17);

-- 给运维角色（role_id=2）授予风控部分菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 12), (2, 13), (2, 14);

-- 给审计角色（role_id=3）授予风控查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 12), (3, 15), (3, 16);

-- 18. 财务管理菜单（menu_id 18~23）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num) VALUES
(18, 0, '财务管理', 0, '/finance', NULL, NULL, 'Coin', 4),
(19, 18, '财务看板', 1, '/finance/dashboard', '/finance/dashboard/index', 'finance:dashboard:list', 'DataBoard', 1),
(20, 18, '交易流水', 1, '/finance/trade', '/finance/trade/index', 'finance:trade:list', 'List', 2),
(21, 18, '日报月报', 1, '/finance/report', '/finance/report/index', 'finance:report:list', 'Document', 3),
(22, 18, '对账管理', 1, '/finance/reconciliation', '/finance/reconciliation/index', 'finance:reconciliation:list', 'Link', 4),
(23, 18, '监管报表', 1, '/finance/regulatory', '/finance/regulatory/index', 'finance:regulatory:list', 'DataAnalysis', 5);

-- 给超级管理员（role_id=1）授予财务菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23);

-- 给运维角色（role_id=2）授予财务部分菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 18), (2, 19), (2, 20);

-- 给审计角色（role_id=3）授予财务查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 18), (3, 21), (3, 22), (3, 23);

-- 24. 运维管理菜单（menu_id 24~30）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num) VALUES
(24, 0, '运维管理', 0, '/ops', NULL, NULL, 'Cpu', 5),
(25, 24, '服务状态监控', 1, '/ops/service', '/ops/service/index', 'ops:service:list', 'Monitor', 1),
(26, 24, '发布管理', 1, '/ops/release', '/ops/release/index', 'ops:release:list', 'Upload', 2),
(27, 24, '配置管理', 1, '/ops/config', '/ops/config/index', 'ops:config:list', 'Setting', 3),
(28, 24, '日志查询', 1, '/ops/log', '/ops/log/index', 'ops:log:search', 'Search', 4),
(29, 24, '告警管理', 1, '/ops/alert', '/ops/alert/index', 'ops:alert:list', 'Warning', 5),
(30, 24, '审计日志', 1, '/ops/audit', '/ops/audit/index', 'ops:audit:list', 'Document', 6);

-- 给超级管理员（role_id=1）授予运维菜单全部权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 24), (1, 25), (1, 26), (1, 27), (1, 28), (1, 29), (1, 30);

-- 给运维角色（role_id=2）授予运维菜单全部权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 24), (2, 25), (2, 26), (2, 27), (2, 28), (2, 29), (2, 30);

-- 给审计角色（role_id=3）授予部分查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 24), (3, 25), (3, 28), (3, 30);

-- 31. CRM管理菜单（menu_id 31~36）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num) VALUES
(31, 0, 'CRM管理', 0, '/crm', NULL, NULL, 'UserFilled', 6),
(32, 31, '客户信息管理', 1, '/crm/customer', '/crm/customer/index', 'crm:customer:list', 'User', 1),
(33, 31, '客户等级管理', 1, '/crm/level', '/crm/level/index', 'crm:level:list', 'Star', 2),
(34, 31, '客户标签管理', 1, '/crm/tag', '/crm/tag/index', 'crm:tag:list', 'Collection', 3),
(35, 31, '客户沟通记录', 1, '/crm/communication', '/crm/communication/index', 'crm:communication:list', 'ChatDotSquare', 4),
(36, 31, '客户反馈处理', 1, '/crm/feedback', '/crm/feedback/index', 'crm:feedback:list', 'Warning', 5);

-- 给超级管理员（role_id=1）授予CRM全部权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36);

-- 给运维角色（role_id=2）授予CRM查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 31), (2, 32), (2, 33), (2, 34);

-- 给审计角色（role_id=3）授予CRM查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 31), (3, 32), (3, 35), (3, 36);
