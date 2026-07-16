#!/bin/bash
#
# git-auto-backup.sh — 自动检测变更并提交备份
# 由 cron 定时触发，每 10 小时运行一次

set -euo pipefail

REPO_DIR="/Users/fangfang/Documents/黑期"
cd "$REPO_DIR"

# 如果有未跟踪或已修改的文件
if [ -n "$(git status --porcelain)" ]; then
    TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"

    # 添加所有变更（包括新文件）
    git add -A

    # 提交
    git commit -m "auto-backup: $TIMESTAMP" 2>/dev/null || true

    # 尝试推送到远程（如果配置了 remote）
    if git remote -v &>/dev/null; then
        git push 2>/dev/null || echo "[backup] push failed (no remote or offline)"
    fi

    echo "[backup] committed at $TIMESTAMP"
else
    echo "[backup] no changes at $(date '+%Y-%m-%d %H:%M:%S')"
fi
