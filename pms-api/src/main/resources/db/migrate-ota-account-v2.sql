-- =====================================================================
-- 迁移：增强 ota_account 表，支持普通直连登录态管理
-- 适用：已有旧库（已有 ota_account 表，无数据时可重建）
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-ota-account-v2.sql
-- =====================================================================

-- 已有表但无数据直接重建
DROP TABLE IF EXISTS ota_account;

CREATE TABLE ota_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL COMMENT 'OTA平台: CTRIP携程/MEITUAN美团/DOUYIN抖音',
  account_name VARCHAR(128) NOT NULL COMMENT 'OTA登录账号',
  password VARCHAR(255) NOT NULL COMMENT 'OTA登录密码(加密存储)',
  merchant_id VARCHAR(128) COMMENT '商家ID/门店ID(部分平台需要)',
  app_key VARCHAR(255) COMMENT '开放平台AppKey(可选,后续API对接用)',
  app_secret VARCHAR(255) COMMENT '开放平台AppSecret(可选)',
  -- 登录态管理
  session_token VARCHAR(1024) DEFAULT NULL COMMENT '加密登录态凭证(session/cookie/token)',
  session_expires_at TIMESTAMP NULL COMMENT '凭证过期时间',
  -- 状态
  status VARCHAR(16) DEFAULT 'DISCONNECTED' COMMENT 'DISCONNECTED未连接/CONNECTED已登录/OFFLINE掉线/ERROR连接异常',
  offline_at TIMESTAMP NULL COMMENT '最近掉线时间',
  offline_reason VARCHAR(255) DEFAULT NULL COMMENT '掉线原因',
  last_error VARCHAR(500) COMMENT '最近错误信息',
  -- 同步
  last_sync_at TIMESTAMP NULL COMMENT '最近一次同步时间',
  -- 自动重连
  auto_reconnect TINYINT DEFAULT 1 COMMENT '开启自动重连:1是/0否',
  retry_count INT DEFAULT 0 COMMENT '当前退避重试次数',
  next_retry_at TIMESTAMP NULL COMMENT '下次自动重试时间',
  -- 保活配置
  keepalive_interval INT DEFAULT 300 COMMENT '保活轮询间隔(秒),默认5分钟',
  -- 风控配置
  server_config VARCHAR(1024) DEFAULT NULL COMMENT '风控配置JSON(UA/设备指纹/出口IP)',
  -- 通用
  enabled TINYINT DEFAULT 1 COMMENT '1启用/0停用',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_platform (hotel_id, platform),
  UNIQUE KEY uk_hotel_platform_account (hotel_id, platform, account_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA直连账号(登录态管理)';
