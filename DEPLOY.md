# PMS 部署指南

本文档覆盖 Docker Compose 生产部署、本地便携 MySQL 验证、TLS、密钥、备份与排错。

## 一、架构

| 服务 | 镜像 | 端口 | 作用 |
|---|---|---|---|
| mysql | mysql:8.0 | 3306 | 业务库，首次启动自动执行 `schema.sql` |
| redis | redis:7-alpine | 6379 | 预留（当前幂等用进程内存储，单实例部署无需；多实例时接入） |
| api | 本地构建 | 8080 | Spring Boot 后端，连 mysql |
| web | 本地构建 | 80/443 | nginx 托管 Vue 静态资源 + 反代 `/api/` 到 api |

数据卷：`mysql_data`（库）、`redis_data`、`api_logs`（应用日志）。

## 二、生产部署（Docker Compose）

### 前置
- Docker Desktop（Windows 需先启用 WSL2 + VirtualMachinePlatform 两个特性并重启）
- `docker compose` 子命令可用（Docker Desktop 自带）

### 步骤
```bash
# 1. 配置密钥（切勿提交 .env）
cp .env.example .env
#   填入：DB_PASS、JWT_SECRET（openssl rand -base64 48）、ADMIN_PASSWORD

# 2. （可选 HTTPS）生成证书
bash scripts/gen-self-signed-cert.sh localhost   # 自签，仅联调
#   生产用 Let's Encrypt：
#   certbot certonly --standalone -d your.domain
#   cp /etc/letsencrypt/live/your.domain/fullchain.pem certs/
#   cp /etc/letsencrypt/live/your.domain/privkey.pem  certs/

# 3. 构建并启动
docker compose up -d --build

# 4. 等 mysql 就绪（compose 已配 healthcheck，api 会等 service_healthy）
docker compose logs -f api        # 看到 Started PmsApplication 即就绪

# 5. 冒烟验证
bash scripts/smoke-test.sh        # 期望 15/15 通过
```

### HTTPS 启用
默认 `docker-compose.yml` 的 web 服务用 `nginx.conf`（HTTP 80）。启用 HTTPS 时：
1. 准备证书到 `./certs/`（见上）
2. 在 `docker-compose.yml` 的 `web.volumes` 挂载 `nginx.https.conf` 与 `certs/`：
   ```yaml
   volumes:
     - ./pms-web/nginx.https.conf:/etc/nginx/conf.d/default.conf:ro
     - ./certs:/etc/nginx/certs:ro
   ```
3. 开放 443：`ports: ["80:80", "443:443"]`
4. `docker compose up -d web`

## 三、密钥与安全

| 环境变量 | 默认值（仅 dev） | 生产要求 |
|---|---|---|
| `DB_PASS` | root | 强密码，与 mysql 容器 `MYSQL_ROOT_PASSWORD` 同源（compose 已联动） |
| `JWT_SECRET` | 占位串 | ≥32 字符随机串，`openssl rand -base64 48` |
| `ADMIN_PASSWORD` | admin | 强密码；或启动后调 `PUT /api/v1/staff/1/password` 修改 |

启动时若检测到默认值，`SecurityWarnings` 会打印醒目告警（不阻断启动）。生产部署必须消除这些告警。

## 四、日志

- 配置：`pms-api/src/main/resources/logback-spring.xml`
- prod profile（compose 默认激活 `SPRING_PROFILES_ACTIVE=prod`）：INFO 级，控制台 + 滚动文件（`/app/logs/pms-api.log`，50MB×14天，1GB 上限）
- SQL 不打印（MyBatis log-impl 已改 slf4j，受级别控制）；调试需 SQL 时用 `SPRING_PROFILES_ACTIVE=dev`
- 容器查看：`docker compose logs -f api`

## 五、备份与恢复

```bash
# 备份（自动识别 Docker 容器或本地便携 MySQL）
bash scripts/backup-mysql.sh ./backups
# 产出：backups/pms-YYYYMMDD-HHMMSS.sql.gz

# 恢复
gunzip -c backups/pms-xxxx.sql.gz | mysql -uroot -p pms
```
生产建议加 cron 定时备份，并将 `.sql.gz` 异地保存。

## 六、本地便携 MySQL（无 Docker 验证用）

见 `scripts/README.md` 与项目记忆。要点：
- 便携 MySQL 8.0.37 位于 `E:\PMS\tools\mysql\`
- 启动：`E:/PMS/tools/mysql/bin/mysqld.exe --standalone --port=3306 --basedir=... --datadir=...`
- 后端连本地 MySQL：不加 `h2` profile，`mvn spring-boot:run` 或 `java -jar target/pms-api-1.0.0.jar`
- H2 profile（`-Dspring-boot.run.profiles=h2`）仅供快速冒烟/单测

## 七、已知限制

- **幂等存储为进程内**：`IdempotencyAspect` 用 `ConcurrentHashMap`，**仅支持单实例 api 部署**。横向扩展前需改造为 redis 存储（compose 已预留 redis 服务）。
- **无 TLS 默认开启**：见上「HTTPS 启用」。
- **admin 默认密码**：见「密钥与安全」。

## 八、排错

| 现象 | 排查 |
|---|---|
| api 启动报连接被拒 | mysql 未就绪；compose 已配 healthcheck，若仍失败查 `docker compose logs mysql` |
| 登录 401 | `ADMIN_PASSWORD` 与实际不符，或 JWT_SECRET 被改后旧 token 失效（重登） |
| web 502 | api 未起；查 `docker compose logs api` |
| 端口占用 3306/8080/80 | 停本机已有服务或改 compose 端口映射 |
| 中文乱码 | 确认 mysql `--character-set-server=utf8mb4`（compose 已设）、连接串 `characterEncoding=utf8`（application.yml 已设） |
