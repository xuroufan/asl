#!/bin/bash
# ============================================================
# 安全审计脚本 — 定期检查系统安全状态
# ============================================================
# 执行: bash security-audit.sh
# 建议: 加入 cron 每天执行一次
# ============================================================

echo "=== 安全审计报告 $(date) ==="
echo ""

# 1. 检查开放端口
echo "── 对外开放端口 ──"
lsof -i -P -n | grep LISTEN | awk '{print $1, $9}' | sort -u

# 2. 检查 Nacos 认证
echo ""
echo "── Nacos 认证状态 ──"
grep "nacos.core.auth.enabled" /Users/fangfang/Documents/黑期/nacos-server/conf/application.properties 2>/dev/null || echo "  未找到Nacos配置"

# 3. 检查 Redis 密码
echo ""
echo "── Redis 密码检查 ──"
grep "requirepass" /Users/fangfang/Documents/黑期/futures-platform/infrastructure/scripts/redis-sentinel.conf 2>/dev/null && echo "  Redis 密码已配置 ✅" || echo "  Redis 密码未配置 ❌"

# 4. 检查 JWT 密钥
echo ""
echo "── JWT 密钥检查 ──"
grep -rn "secret:" /Users/fangfang/Documents/黑期/futures-platform/infrastructure/nacos-config/ 2>/dev/null | grep -v "placeholder" | head -5

# 5. 检查 HTTPS 配置
echo ""
echo "── HTTPS 证书 ──"
ls -la /Users/fangfang/Documents/黑期/futures-platform/infrastructure/certs/ 2>/dev/null | grep -v "total\|^$"

# 6. 检查最近登录日志
echo ""
echo "── 最近登录失败记录 ──"
tail -5 /tmp/futures-logs/*/application.log 2>/dev/null | grep -i "login\|认证\|失败" || echo "  无登录日志"

echo ""
echo "=== 审计完成 ==="
