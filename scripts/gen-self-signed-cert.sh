#!/usr/bin/env bash
# 生成本地自签 TLS 证书，用于 HTTPS 联调（生产请改用 Let's Encrypt）。
# 用法： bash scripts/gen-self-signed-cert.sh [域名/IP]
# 产出： ./certs/fullchain.pem 与 ./certs/privkey.pem
set -eu
DOMAIN="${1:-localhost}"
CERTS_DIR="${CERTS_DIR:-./certs}"

mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

# openssl 可用性检查
command -v openssl >/dev/null 2>&1 || { echo "需要 openssl"; exit 1; }

openssl req -x509 -newkey rsa:2048 -nodes -days 825 \
  -keyout privkey.pem -out fullchain.pem \
  -subj "/CN=$DOMAIN" \
  -addext "subjectAltName=DNS:$DOMAIN,IP:127.0.0.1" 2>/dev/null

echo "已生成自签证书（CN=$DOMAIN）："
echo "  $CERTS_DIR/fullchain.pem"
echo "  $CERTS_DIR/privkey.pem"
echo "浏览器首次访问会提示不信任，确认继续即可。"
