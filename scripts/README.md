# 冒烟测试

## 前置条件
本机或服务器已安装 Docker，并已启动全栈：
```bash
docker compose up -d --build
# 等待约 60s 让 MySQL 初始化 + API 启动完成
```

确认服务就绪：
```bash
curl http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'
```

## 运行
```bash
bash scripts/smoke-test.sh
```
依赖：`curl`、`python3`（用于解析 JSON；若装了 `jq` 可自行替换）。

### 数据幂等性（持久化 MySQL 上可重复跑）
冒烟开头默认调用 `scripts/reset-data.sh` 清空业务表 + 重置房态，保证在持久化 MySQL
上连续多次运行都 15/15（H2 内存库每次启动本就全新，reset 脚本会自动检测并跳过）。

- 自动识别 `pms-mysql` Docker 容器 / 本地便携 MySQL（`E:\PMS\tools\mysql`）
- 单独重置：`bash scripts/reset-data.sh`
- 跳过重置：`SKIP_RESET=1 bash scripts/smoke-test.sh`

## 测试覆盖的链路
1. 登录拿 token
2. 读取全局配置
3. 房号列表 / 查净房
4. 新建预订（带 `Idempotency-Key`，房型明细落库）
5. 幂等命中校验
6. 确认预订
7. 办理入住（占用净房 → 建 stay + folio + 押金）
8. 按 stay 查账单
9. 录消费 + 收款（关账校验）
10. 退房（释放房态 → 预订转 CHECKED_OUT）
11. 房态看板
12. 夜审草稿 + 营收日报 + 出租率报表

## 预期输出
末尾打印 `通过: N    失败: 0` 并 `🎉 冒烟测试全部通过`。

## 手动验证 WebSocket 房态推送
浏览器控制台：
```js
const ws = new WebSocket('ws://localhost:8080/api/v1/ws/room-status');
ws.onmessage = e => console.log(JSON.parse(e.data));
```
随后在房态页操作换房/报修，应看到推送消息。
