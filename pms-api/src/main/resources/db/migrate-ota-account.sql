-- =====================================================================
-- 迁移：新增 ota_account 表（OTA 账号直连：携程/美团/抖音）
-- 适用：已有旧库（无 ota_account 表）
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-ota-account.sql
-- 注：MySQL 8 支持 CREATE TABLE IF NOT EXISTS，幂等可重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS ota_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL COMMENT 'OTA平台: CTRIP携程/MEITUAN美团/DOUYIN抖音',
  account_name VARCHAR(128) NOT NULL COMMENT 'OTA登录账号',
  password VARCHAR(255) NOT NULL COMMENT 'OTA登录密码(加密存储)',
  merchant_id VARCHAR(128) COMMENT '商家ID/门店ID(部分平台需要)',
  app_key VARCHAR(255) COMMENT '开放平台AppKey(可选,后续API对接用)',
  app_secret VARCHAR(255) COMMENT '开放平台AppSecret(可选)',
  status VARCHAR(16) DEFAULT 'DISCONNECTED' COMMENT 'DISCONNECTED未连接/CONNECTED已连接/ERROR连接异常',
  last_sync_at TIMESTAMP NULL COMMENT '最近一次同步时间',
  last_error VARCHAR(500) COMMENT '最近错误信息',
  enabled TINYINT DEFAULT 1 COMMENT '1启用/0停用',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_platform (hotel_id, platform),
  UNIQUE KEY uk_hotel_platform_account (hotel_id, platform, account_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA直连账号';
