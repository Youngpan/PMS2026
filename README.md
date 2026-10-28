# 酒店 PMS（全栈）

![CI](https://github.com/OWNER/pms/actions/workflows/ci.yml/badge.svg)

轻量自托管酒店管理系统。对应产品文档见 `PMS产品开发文档.md`。

## 目录结构
```
E:\PMS\
├─ PMS产品开发文档.md          PRD
├─ 01_数据库表结构设计.md
├─ 02_API接口定义.md
├─ 03_页面流转与UI原型.md
├─ 04_技术选型方案.md
├─ docker-compose.yml          mysql + redis + api + web 一键起
├─ pms-api\                    Spring Boot 3 + MyBatis-Plus 后端
│  ├─ README.md
│  ├─ pom.xml / Dockerfile
│  └─ src\main\java\com\pms\   auth/room/reservation/stay/folio/housekeeping/report/system
└─ pms-web\                    Vue3 + Vite + Element Plus 前端
   ├─ README.md
   ├─ package.json / Dockerfile / nginx.conf
   └─ src\                     api/stores/router/layout/views
```

## 一键启动（Docker）
```bash
docker compose up -d --build
```
- 前端：http://localhost
- 后端 API：http://localhost:8080/api/v1
- Swagger：http://localhost:8080/api/v1/swagger-ui.html
- 默认账号：admin / admin

## 本地开发
```bash
# 后端
cd pms-api && mvn spring-boot:run
# 前端
cd pms-web && npm install && npm run dev
```

## CI / 测试
推送或 PR 到 `main` 时，[`.github/workflows/ci.yml`](.github/workflows/ci.yml) 自动跑两个并行 job：
- **api-test**：`mvn test`（测试类用 `@ActiveProfiles("h2")` 走 H2 内存库，无需 MySQL）
- **web-build**：`npm ci && npm run build`（vue-tsc 类型检查 + vite 构建）

本地复刻：`cd pms-api && mvn test`（31 个测试）/ `cd pms-web && npm run build`。

浏览器 e2e（预订→确认→入住→退房全链路，puppeteer-core 驱动真实 Chrome）：
- 本地：`bash scripts/run-e2e-flow.sh`（前置 MySQL+后端+前端 在跑，Chrome 已装）
- CI：`.github/workflows/e2e.yml` 手动触发（`workflow_dispatch`），runner 自带 Chrome，后端用 h2 内存库

前端开发走 Vite 代理 `/api` → `http://localhost:8080`。

## 技术栈
| 层 | 技术 |
|---|---|
| 前端 | Vue3 + TS + Vite + Element Plus + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus + JWT + Hutool + springdoc |
| 数据 | MySQL 8 + Redis 7 |
| 部署 | Docker Compose + Nginx |

## 已实现模块（8/8）
鉴权(JWT+bcrypt) · 房型/房号 · 预订(分页列表/详情/房型明细/排房) · 前台(入住/退房/续住/换房/调价) · 账务(账单/明细/押金/收款/转账/分账/收据) · 房务(房态看板/派单/报修) · 夜审与报表(夜审确认/营收日报/每日趋势/客账余额/出租率ADR RevPAR) · 系统管理(部门/员工/角色权限/操作日志/全局参数) + Swagger + 房态 WebSocket 实时推送。
