# pms-web (Vue3 + Vite + Element Plus)

酒店 PMS 前端，对接 `pms-api` 后端。

## 技术栈
- Vue 3 + TypeScript + Vite
- Element Plus（自动按需引入）+ Pinia + Vue Router + Axios
- ECharts（报表预留）

## 启动
```bash
npm install
npm run dev        # http://localhost:5173
```
开发环境通过 Vite 代理 `/api` → `http://localhost:8080`，需先启动后端。

## 构建
```bash
npm run build     # 产物 dist/
```

## 默认账号
admin / admin（后端首次启动自动初始化）

## 页面与路由
| 路由 | 页面 | 对应接口 |
|---|---|---|
| /login | 登录 | POST /auth/login |
| /dashboard | 仪表盘(夜审草稿) | GET/POST /reports/night-audit* |
| /reservations | 预订管理 | /reservations |
| /stays | 在住看板(入住/换房/退房) | /stays |
| /housekeeping | 房态看板(派单/报修) | /housekeeping |
| /rooms | 房型与房号(新增/编辑/批量生成) | /room-types、/rooms |
| /folios/:id | 账单(消费/押金/收款/收据) | /folios |
| /reports | 营收/出租率/客账余额 | /reports/* |
| /system/staff | 员工管理 | /staff |

## 说明
- Axios 拦截器统一处理 `{code,message,data}`，401 跳登录。
- Token 存 localStorage，请求头自动加 `Authorization: Bearer`。
- 路由守卫：未登录跳 /login。
- 组件按需引入由 unplugin-vue-components 自动完成，无需手动 import Element Plus 组件。
