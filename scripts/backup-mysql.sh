#!/usr/bin/env bash
# 备份 PMS MySQL 数据库。同时支持：
#   - Docker 部署：通过 docker exec 进入 pms-mysql 容器导出
#   - 本地便携 MySQL：直接调用 mysql 客户端
# 用法： bash scripts/backup-mysql.sh [output-dir]
set -eu
OUT_DIR="${1:-./backups}"
mkdir -p "$OUT_DIR"

TS=$(date +%Y%m%d-%H%M%S)
FILE="$OUT_DIR/pms-$TS.sql.gz"

# 优先 Docker 容器
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^pms-mysql$'; then
  echo "通过 Docker 容器 pms-mysql 导出..."
  docker exec pms-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers pms' | gzip > "$FILE"
else
  # 退回本地便携 MySQL（见 tools/mysql）
  MYSQL="${MYSQL:-/e/PMS/tools/mysql/bin/mysqldump.exe}"
  [ -x "$MYSQL" ] || { echo "未找到 mysqldump：$MYSQL；可经 MYSQL= 环境变量指定"; exit 1; }
  echo "通过本地 mysqldump 导出..."
  "$MYSQL" -h127.0.0.1 -P3306 -uroot -proot \
    --single-transaction --routines --triggers --default-character-set=utf8mb4 pms | gzip > "$FILE"
fi

SIZE=$(du -h "$FILE" | cut -f1)
echo "备份完成：$FILE ($SIZE)"
echo "恢复示例： gunzip -c $FILE | mysql -uroot -proot pms"
