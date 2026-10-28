# PMS-API (Spring Boot 3 + MyBatis-Plus)

酒店 PMS 后端服务，对应文档见上级目录 `PMS产品开发文档.md`。

## 环境
- JDK 17
- Maven 3.9+
- MySQL 8.0
- Redis 7（可选，预留）

## 本地启动

```bash
# 1. 启动 MySQL（或用 docker-compose）
docker compose up -d mysql

# 2. 建表（schema.sql 在容器启动时自动执行，或手动导入）
mysql -h127.0.0.1 -uroot -proot pms < src/main/resources/db/schema.sql

# 3. 运行
mvn spring-boot:run
```

服务地址：http://localhost:8080/api/v1

## 一键 Docker 启动（含 MySQL + Redis + API）
```bash
cd <repo-root>
docker compose up -d --build
```

## 默认账号
- 用户名：`admin`
- 密码：占位（`AuthController` 为桩实现，生产需对接 staff 表 + bcrypt）

## 接口示例
```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'

# 房态看板（带 token）
curl http://localhost:8080/api/v1/rooms \
  -H 'Authorization: Bearer <token>'
```

## 模块进度
| 模块 | 状态 |
|---|---|
| 鉴权(JWT + bcrypt 登录) | ✅ |
| 房型/房号 | ✅ |
| 预订(分页列表/详情/房型明细落库/排房) | ✅ |
| 前台接待(入住/退房/续住/换房/调价) | ✅ |
| 账务(账单/明细/押金/收款/转账/分账/收据) | ✅ |
| 房务(房态看板/清扫派单/维修报修) | ✅ |
| 夜审/报表(夜审确认/营收日报/每日趋势/客账余额/出租率ADR RevPAR) | ✅ |
| 系统管理(部门/员工/角色权限/操作日志/全局参数) | ✅ |
| Swagger(springdoc-openapi) | ✅ |
| 房态实时推送(WebSocket /ws/room-status) | ✅ |

## Swagger
启动后访问：http://localhost:8080/api/v1/swagger-ui.html

## WebSocket
房态变更实时推送：`ws://<host>/api/v1/ws/room-status`，消息格式：
`{"type":"room-status","roomId":1,"number":"101","status":"VC","ts":"..."}`

## 待办
- （暂无核心待办；可按需扩展多实例 Redis 幂等、房态 WS 鉴权等）
