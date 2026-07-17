-- ============================================================
-- 期货交易平台 — 数据库索引优化 DDL
-- 执行: mysql -u root -p futures_order < optimize-indexes.sql
-- ============================================================

-- t_order: 用户+状态复合索引 (订单列表)
CREATE INDEX IF NOT EXISTS idx_order_user_status ON t_order (user_id, status);

-- t_order: 用户+时间排序索引 (历史委托)
CREATE INDEX IF NOT EXISTS idx_order_user_created ON t_order (user_id, created_at DESC);

-- t_order: 撮合引擎筛选索引 (只索引活跃订单, 减少索引体积)
CREATE INDEX IF NOT EXISTS idx_order_status_price ON t_order (status, limit_price)
    WHERE status IN ('PENDING', 'PARTIALLY_FILLED');

-- t_fund_account: 用户资金账户
CREATE UNIQUE INDEX IF NOT EXISTS idx_fund_user ON t_fund_account (user_id);

-- t_position: 用户+合约持仓查询
CREATE INDEX IF NOT EXISTS idx_position_user_symbol ON t_position (user_id, symbol);

-- 验证索引
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('t_order','t_fund_account','t_position')
ORDER BY tablename, indexname;
