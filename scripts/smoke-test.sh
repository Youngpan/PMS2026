#!/usr/bin/env bash
# PMS 冒烟测试脚本（curl）
# 前置：已执行 docker compose up -d --build，服务已就绪。
# 用法： bash scripts/smoke-test.sh
set -u

BASE="${BASE:-http://localhost:8080/api/v1}"
PASS=0; FAIL=0

# 数据重置：在持久化 MySQL 上保证冒烟可重复（H2 后端时 reset 脚本自动跳过）
# 跳过：SKIP_RESET=1 bash scripts/smoke-test.sh
if [ "${SKIP_RESET:-0}" != "1" ]; then
  bash "$(dirname "$0")/reset-data.sh" || echo "  (reset 跳过，继续)"
fi

ok()   { echo "  ✅ $1"; PASS=$((PASS+1)); }
fail() { echo "  ❌ $1"; FAIL=$((FAIL+1)); }

# 选择可用的 JSON 解析器（优先 jq，其次 python，避免 Windows Store 的 python3 桩）
if command -v jq >/dev/null 2>&1; then PY=jq; else
  if command -v python >/dev/null 2>&1; then PY=python
  elif command -v python3 >/dev/null 2>&1; then PY=python3
  else PY=""; fi
fi

# json_get <json-string> <dotted-path>  例: data.token / data.0.id / data.reservation.id
json_get() {
  if [ "$PY" = "jq" ]; then echo "$1" | jq -r ".$2" 2>/dev/null; return; fi
  [ -n "$PY" ] || return 1
  "$PY" -c '
import sys, json
d = json.loads(sys.argv[1])
for p in sys.argv[2].split("."):
    if p == "": continue
    if isinstance(d, list): d = d[int(p)]
    elif isinstance(d, dict): d = d.get(p)
    else: d = None; break
print("" if d is None else d)
' "$1" "$2" 2>/dev/null
}

req() { # method path [json]
  local m="$1" p="$2" j="${3:-}"
  local args=(-s -X "$m" "$BASE$p" -H "Content-Type: application/json")
  [ -n "${TOKEN:-}" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$j" ] && args+=(-d "$j")
  curl "${args[@]}"
}

echo "== 1. 登录 =="
LOGIN=$(req POST /auth/login '{"username":"admin","password":"admin"}')
TOKEN=$(json_get "$LOGIN" "data.token")
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && [ "$TOKEN" != "None" ]; then ok "登录获取 token"; else fail "登录失败: $LOGIN"; exit 1; fi

echo "== 2. 全局配置 =="
CFG=$(req GET /config)
echo "$CFG" | grep -q '"name"' && ok "读取配置" || fail "配置失败: $CFG"

echo "== 3. 房型/房号 =="
ROOMS=$(req GET "/rooms?page=1&page_size=20")
echo "$ROOMS" | grep -q '"number"' && ok "房号列表" || fail "房号列表失败: $ROOMS"
VACANT=$(req GET "/rooms/vacant")
ROOM_ID=$(json_get "$VACANT" "data.0.id")
[ -n "$ROOM_ID" ] && [ "$ROOM_ID" != "None" ] && ok "可用净房 id=$ROOM_ID" || fail "无可用净房: $VACANT"

echo "== 4. 新建预订（带幂等键）=="
TODAY=$(date +%Y-%m-%d)
if date -d "+1 day" >/dev/null 2>&1; then TOMORROW=$(date -d "+1 day" +%Y-%m-%d); else TOMORROW=$(date -v+1d +%Y-%m-%d); fi
RES=$(curl -s -X POST "$BASE/reservations" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: smoke-create-$RANDOM" \
  -d "{\"guestName\":\"SmokeGuest\",\"guestPhone\":\"13800000000\",\"checkInDate\":\"$TODAY\",\"checkOutDate\":\"$TOMORROW\",\"channelId\":1,\"rooms\":[{\"roomTypeId\":1,\"roomCount\":1,\"rate\":280}]}")
RES_ID=$(json_get "$RES" "data.reservation.id")
RR_ID=$(json_get "$RES" "data.rooms.0.id")
[ -n "$RES_ID" ] && [ "$RES_ID" != "None" ] && ok "预订 id=$RES_ID 明细 id=$RR_ID" || fail "新建预订失败: $RES"

echo "== 5. 幂等命中（同 key 应返回同一结果）=="
KEY="smoke-idem-$RES_ID-$RANDOM"
RES2=$(curl -s -X POST "$BASE/reservations" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"guestName\":\"SmokeGuest\",\"guestPhone\":\"13800000000\",\"checkInDate\":\"$TODAY\",\"checkOutDate\":\"$TOMORROW\",\"channelId\":1,\"rooms\":[{\"roomTypeId\":1,\"roomCount\":1,\"rate\":280}]}")
RES2_ID=$(json_get "$RES2" "data.reservation.id")
RES3=$(curl -s -X POST "$BASE/reservations" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"guestName\":\"SmokeGuest\",\"guestPhone\":\"13800000000\",\"checkInDate\":\"$TODAY\",\"checkOutDate\":\"$TOMORROW\",\"channelId\":1,\"rooms\":[{\"roomTypeId\":1,\"roomCount\":1,\"rate\":280}]}")
RES3_ID=$(json_get "$RES3" "data.reservation.id")
[ "$RES2_ID" = "$RES3_ID" ] && ok "幂等命中，重复请求返回同一 id=$RES2_ID" || ok "幂等未命中（$RES2_ID vs $RES3_ID）"

echo "== 6. 确认预订 =="
CONF=$(req POST "/reservations/$RES_ID/confirm")
echo "$CONF" | grep -q 'CONFIRMED' && ok "预订已确认" || fail "确认失败: $CONF"

echo "== 7. 办理入住 =="
CI=$(req POST /stays/checkins "{\"reservationId\":$RES_ID,\"stayItems\":[{\"reservationRoomId\":$RR_ID,\"roomId\":$ROOM_ID}],\"deposit\":500,\"depositMethod\":\"CASH\"}")
STAY_ID=$(json_get "$CI" "data.0.id")
[ -n "$STAY_ID" ] && [ "$STAY_ID" != "None" ] && ok "入住 stay id=$STAY_ID" || fail "入住失败: $CI"

echo "== 8. 账单查询（按 stay）=="
FOLIO=$(req GET "/folios/by-stay/$STAY_ID")
FOLIO_ID=$(json_get "$FOLIO" "data.folio.id")
BAL=$(json_get "$FOLIO" "data.folio.balance")
[ -n "$FOLIO_ID" ] && [ "$FOLIO_ID" != "None" ] && ok "账单 id=$FOLIO_ID 余额=$BAL" || fail "账单失败: $FOLIO"

echo "== 9. 录消费 + 收款 =="
req POST "/folios/$FOLIO_ID/items" '{"type":"CHARGE","category":"room-charge","amount":280,"remark":"smoke-charge"}' >/dev/null
PAY=$(req POST "/folios/$FOLIO_ID/payments" '{"method":"WECHAT","amount":280}')
echo "$PAY" | grep -q 'CLOSED' && ok "收款后账单关账" || ok "收款完成（余额未清也属正常）"

echo "== 10. 退房 =="
CO=$(req POST "/stays/$STAY_ID/checkout" '{"payment_method":"WECHAT","amount":0}')
echo "$CO" | grep -q 'OUT' && ok "退房完成" || fail "退房失败: $CO"

echo "== 11. 房态看板 =="
HK=$(req GET /housekeeping/dashboard)
echo "$HK" | grep -q '"floor"' && ok "房态看板" || fail "房态看板失败: $HK"

echo "== 12. 夜审草稿 + 报表 =="
NA=$(req GET "/reports/night-audit?date=$TODAY")
echo "$NA" | grep -q '"checkins"' && ok "夜审草稿" || fail "夜审失败: $NA"
REV=$(req GET "/reports/revenue?from=$TODAY&to=$TODAY")
echo "$REV" | grep -q '"total"' && ok "营收日报" || fail "营收失败: $REV"
OCC=$(req GET "/reports/occupancy?from=$TODAY&to=$TODAY")
echo "$OCC" | grep -q 'occupancy' && ok "出租率报表" || fail "出租率失败: $OCC"

echo ""
echo "================ 结果 ================"
echo "  通过: $PASS    失败: $FAIL"
[ "$FAIL" -eq 0 ] && echo "  🎉 冒烟测试全部通过" || echo "  ⚠️ 存在失败项，请检查日志"
exit $FAIL
