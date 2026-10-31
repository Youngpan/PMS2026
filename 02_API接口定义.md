# API 接口定义

> RESTful 风格，基础路径 `/api/v1`，统一返回结构：
> ```json
> { "code": 0, "message": "ok", "data": {} }
> ```
> 鉴权：`Authorization: Bearer <jwt>`。所有列表接口支持分页 `page`/`page_size`。

---

## 0. 通用状态码
| code | 含义 |
|---|---|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 不存在 |
| 409 | 状态冲突(如房态不可入住) |
| 500 | 服务错误 |

---

## 1. 鉴权（M7）

### POST /auth/login
登录获取 token。
- 入参：`{ "username": "", "password": "" }`
- 出参：`{ "token": "", "staff": { "id":1, "real_name":"", "roles":[] } }`

### POST /auth/logout
登出（吊销 token）。

### GET /auth/profile
当前用户信息与权限点。

---

## 2. 房型与房号（M5）

### GET /room-types
房型列表。

### POST /room-types
创建房型。
- 入参：`{ "code":"", "name":"", "max_occupancy":2, "bed_type":"", "area":0, "base_rate":0, "rack_rate":0 }`

### PUT /room-types/{id}
修改房型，入参同创建（仅传需要改的字段即可）。基础价变更会向 OTA 渠道推送 `RATE_CHANGED` 事件。

### GET /rooms
房号列表，分页 + 筛选：`?page=1&page_size=20&rent_status=OPEN&clean_status=CLEAN&room_type_id=1`。
返回 `{ list, total, page, pageSize }`，排序按楼层、房号升序。

### POST /rooms
创建房号：`{ "room_type_id":1, "number":"101", "floor":1, "remark":"" }`
- 房号在本酒店内唯一，重复返回 `409`。
- 房型必须存在，否则返回 `400`。

### POST /rooms/batch
批量创建房号（一次录入整层）。
- 入参：`{ "room_type_id":1, "floor":3, "numbers":["301","302"], "remark":"" }`
- 整批校验：房号非空、批内不重复、库内不重复；任一不满足则整批不写入（`409` 带重复房号列表）。
- `floor` 可省略，省略时按房号推断（`301` → 3，`1208` → 12）。

### PUT /rooms/{id}
修改房号基础信息：`{ "number":"301A", "floor":3, "room_type_id":1, "remark":"" }`
- 只更新传入的字段；改房号同样做唯一性校验（`409`）。
- 房态变更请用下面的 rent-status / clean-status 接口。

### PUT /rooms/{id}/rent-status
可售状态：`{ "rent_status":"OPEN|CLOSED|ASSIGNED" }`（打开 / 关闭 / 已排房在住）。

### PUT /rooms/{id}/clean-status
卫生状态：`{ "clean_status":"CLEAN|DIRTY" }`（净房 / 脏房）。

### GET /rooms/vacant?room_type_id=
可选房房号（打开 + 净房），供办理入住选房。

---

## 3. 预订（M1）

### GET /reservations?status=&date_from=&date_to=&phone=
预订列表。

### POST /reservations
创建预订。
```json
{
  "guest": { "name":"", "phone":"", "gender":0 },
  "check_in_date":"2026-11-01",
  "check_out_date":"2026-11-03",
  "channel_id":1,
  "rooms": [
    { "room_type_id":1, "room_count":1, "rate":280 }
  ],
  "remark":""
}
```

### GET /reservations/{id}
预订详情。

### PUT /reservations/{id}
修改预订（日期/房型/房数/备注）。

### POST /reservations/{id}/cancel
取消预订：`{ "reason":"" }`

### POST /reservations/{id}/assign-room
排房：`{ "items":[ {"reservation_room_id":1, "room_id":101} ] }`

---

## 4. 前台接待（M2）

### POST /checkins
办理入住。
```json
{ "reservation_id":1, "stay_items":[ {"reservation_room_id":1, "room_id":101} ], "deposit":500, "deposit_method":"CASH" }
```
- 校验：房号必须为 VC 且未被他住。
- 出参：`{ "stay_ids":[1], "folio_id":10 }`

### GET /stays?status=IN
在住列表。

### POST /stays/{id}/extend
续住：`{ "extend_to_date":"2026-11-04" }`

### POST /stays/{id}/transfer-room
换房：`{ "to_room_id":102, "reason":"" }`

### PUT /stays/{id}/rate
调整房价：`{ "rate":260 }`

### POST /stays/{id}/checkout
退房结算。
- 入参：`{ "payment_method":"WECHAT", "amount":560 }`
- 流程：汇总 folio → 收款 → 退押金 → 释放房态 VD。
- 出参：`{ "folio_id":10, "paid":560, "refund":440 }`

---

## 5. 账务（M4）

### GET /folios/{id}
账单详情含明细。

### POST /folios/{id}/items
手工录入消费/押金（房费也走此接口）。
```json
{ "type":"CHARGE", "category":"房费", "amount":280, "remark":"11-01房费" }
```

### POST /folios/{id}/transfer
转账：`{ "to_folio_id":12, "amount":100, "items":[11,12] }`

### POST /folios/{id}/split
分账：`{ "items":[ {"item_id":11, "to_folio_id":12, "amount":100} ] }`

### POST /folios/{id}/payments
支付结算：`{ "method":"WECHAT", "amount":560 }`

### POST /folios/{id}/receipt
打印收据（返回可打印文本/PDF）。

---

## 6. 房务（M3）

### GET /housekeeping/dashboard
房态看板：按楼层聚合房态。

### GET /housekeeping/calendar?from=&to=
房态日历：列=房间，行=日期。返回 `{ rooms, dates, cells }`，`cells` 的 key 为 `"<roomId>:<date>"`，仅包含被在住 / 预订 / 封房覆盖的格子。

### GET /housekeeping/tasks?status=PENDING&assignee_id=
清扫任务列表，按创建时间升序。

### POST /housekeeping/tasks
派单：`{ "room_id":1, "type":"DAILY|CHECKOUT_CLEAN|DEEP_CLEAN", "assignee_id":5 }`（`assignee_id` 可空）
- 房号不存在 → `404`；类型非法 → `400`；同房已有 PENDING 任务 → `409`。

### PUT /housekeeping/tasks/{id}/finish
完成清扫：任务置 `DONE`；房间若为脏房则置净房（`CLEAN`）。非 PENDING → `409`。

### GET /housekeeping/maintenance?status=OPEN
维修单列表，按报修时间倒序。

### POST /housekeeping/maintenance
报修：`{ "room_id":1, "issue":"空调不制冷" }`
- 报修后该房可售状态置 `CLOSED`（不可售）。
- 内容为空 → `400`；同房已有 OPEN 维修单 → `409`。

### PUT /housekeeping/maintenance/{id}/fix
修复：维修单置 `FIXED`；房间若为 `CLOSED` 则恢复 `OPEN`（可售）。

### POST /housekeeping/blocks
按日期范围封房（不影响整房可售状态）：`{ "room_id":1, "start":"2026-11-01", "end":"2026-11-03", "reason":"维修" }`
- 与该房已有 ACTIVE 封房重叠 → `409`。

### PUT /housekeeping/blocks/{id}/release
释放封房：置 `RELEASED`，对应日期恢复可售。

---

## 7. 夜审与报表（M6）

### GET /reports/night-audit?date=
夜审草稿：`{ "checkins":3, "checkouts":2, "revenue":840, "items":[...] }`

### POST /reports/night-audit/confirm
确认夜审：`{ "date":"2026-10-26" }`

### GET /reports/revenue?from=&to=
营业收入日报。

### GET /reports/folio-balance
客账余额表（未结 folio）。

### GET /reports/occupancy?from=&to=
出租率/ADR/RevPAR：`{ "occupancy":0.82, "adr":285, "revpar":234 }`

---

## 8. 系统管理（M7）

### GET /departments / POST /departments
### GET /staff / POST /staff / PUT /staff/{id}
### GET /roles / POST /roles / PUT /roles/{id}/permissions
### GET /operation-logs?staff_id=&module=&from=
### GET /config / PUT /config  （全局参数）
### POST /backup  （触发数据备份）

---

## 9. 接口集成（M8）

### POST /interface/ota/pull
拉取 OTA 订单（定时调用）。

### POST /interface/pos/charge
POS 消费入客房账：`{ "folio_id":10, "amount":88, "category":"餐饮" }`

### GET /interface/logs
接口日志查询。

---

## 10. 接口约定补充
- 时间字段统一 ISO 8601（`2026-10-26T12:00:00+08:00`）。
- 金额均为元，两位小数。
- 写操作幂等：带 `Idempotency-Key` 头。
- 列表分页响应：`{ "list":[], "total":0, "page":1, "page_size":20 }`
