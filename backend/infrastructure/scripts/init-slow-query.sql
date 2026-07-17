-- ============================================================
-- MySQL 慢查询日志 + 性能基线配置
-- ============================================================
-- 执行: mysql -u root -p < init-slow-query.sql
-- 持续监控: SHOW ENGINE INNODB STATUS\G

-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL slow_query_log_file = '/var/log/mysql/mysql-slow.log';
SET GLOBAL long_query_time = 1;           -- 超过1秒记录
SET GLOBAL log_queries_not_using_indexes = ON;  -- 记录未使用索引的查询
SET GLOBAL log_slow_admin_statements = ON;       -- 记录管理语句
SET GLOBAL log_throttle_queries_not_using_indexes = 10;  -- 每分钟最多10条

-- 分析慢查询（执行后查看）
-- mysqldumpslow -s t -t 10 /var/log/mysql/mysql-slow.log

-- 查询当前配置
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
