#!/usr/bin/env bash
# 前端全链路 e2e：reset 数据到 VC 净房 → 驱动真实 Chrome 走 预订→确认→入住→退房。
# 前置：MySQL(3306)+后端(8080)+前端(5173) 在跑，Chrome 已装，puppeteer-core 已装（pms-web/node_modules）。
# 用法：bash scripts/run-e2e-flow.sh
set -e
cd "$(dirname "$0")/.."
echo "=== 1. reset 数据 ==="
bash scripts/reset-data.sh
echo "=== 2. 跑 e2e 全链路 ==="
cd pms-web
node e2e-flow.mjs
