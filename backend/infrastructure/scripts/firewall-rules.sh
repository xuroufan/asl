#!/bin/bash
# ============================================================
# 防火墙规则 — 生产环境端口隔离
# ============================================================
# 执行: sudo bash firewall-rules.sh
# ============================================================

set -euo pipefail

# ─── 清理旧规则 ───
iptables -F INPUT
iptables -P INPUT DROP

# ─── 允许回环 ───
iptables -A INPUT -i lo -j ACCEPT

# ─── 允许已建立的连接 ───
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# ─── 允许 SSH ───
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# ─── 对外暴露的端口 (公网) ───
iptables -A INPUT -p tcp --dport 443 -j ACCEPT   # HTTPS
iptables -A INPUT -p tcp --dport 80 -j ACCEPT    # HTTP 重定向

# ─── 对内暴露的端口 (内网/VPN) ───
iptables -A INPUT -p tcp --dport 8848 -s 10.0.0.0/8 -j ACCEPT  # Nacos
iptables -A INPUT -p tcp --dport 3306 -s 10.0.0.0/8 -j ACCEPT  # MySQL
iptables -A INPUT -p tcp --dport 6379 -s 10.0.0.0/8 -j ACCEPT  # Redis
iptables -A INPUT -p tcp --dport 9876 -s 10.0.0.0/8 -j ACCEPT  # RocketMQ
iptables -A INPUT -p tcp --dport 9090 -s 10.0.0.0/8 -j ACCEPT  # Prometheus
iptables -A INPUT -p tcp --dport 3002 -s 10.0.0.0/8 -j ACCEPT  # Grafana

# ─── 拒绝其余 ───
iptables -A INPUT -j DROP

echo "✅ 防火墙规则已应用"
echo "   公网暴露: 仅 80/443"
echo "   内网访问: Nacos/MySQL/Redis/RocketMQ/Monitoring"
