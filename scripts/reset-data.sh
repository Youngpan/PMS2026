#!/usr/bin/env bash
# 重置 PMS 业务数据（保留种子），让冒烟测试在持久化 MySQL 上可重复运行。
# 自动识别：Docker 容器 pms-mysql / 本地便携 MySQL / 都没有则跳过（兼容 H2 后端）。
# 用法： bash scripts/reset-data.sh
set -eu

SQL_FILE="$(dirname "$0")/reset-data.sql"

run_sql() { # <client-invoker...>
  if [ "$#" -gt 0 ]; then "$@"; return $?; fi
  return 1
}

# 1) Docker 容器优先
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^pms-mysql$'; then
  echo "reset: 通过 Docker 容器 pms-mysql"
  docker exec -i pms-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" pms' < "$SQL_FILE"
  echo "reset: 完成（Docker）"
  exit 0
fi

# 2) 本地便携 MySQL（见 tools/mysql）
MYSQL_BIN="${MYSQL:-/e/PMS/tools/mysql/bin/mysql.exe}"
if [ -x "$MYSQL_BIN" ]; then
  # 探测 3306 是否在听
  if powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1" 2>/dev/null | grep -q LocalPort 2>/dev/null \
     || "$MYSQL_BIN" -h127.0.0.1 -P3306 -uroot -proot -e "SELECT 1" >/dev/null 2>&1; then
    echo "reset: 通过本地便携 MySQL"
    "$MYSQL_BIN" -h127.0.0.1 -P3306 -uroot -proot --default-character-set=utf8mb4 pms < "$SQL_FILE" 2>/dev/null
    echo "reset: 完成（本地 MySQL）"
    exit 0
  fi
fi

# 3) 都没有 —— 大概率后端跑在 H2 内存库，每次启动数据本就是全新的
echo "reset: 未发现可用 MySQL（Docker/本地均无）。若后端为 H2 profile，数据本就每次重建，无需重置。"
exit 0
