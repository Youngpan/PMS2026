-- 便携 MySQL 8 默认 root@localhost 为 caching_sha2_password，
-- 而 application.yml 的 JDBC URL（useSSL=false，无 allowPublicKeyRetrieval）
-- 连不上（"Public Key Retrieval is not allowed"）。
-- 还原上个会话的工作状态：root 改回 mysql_native_password。
-- 仅本地便携库，不影响生产。执行：mysql -h127.0.0.1 -uroot -proot pms < scripts/fix-root-auth.sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
FLUSH PRIVILEGES;
SELECT user, host, plugin FROM mysql.user WHERE user='root';
