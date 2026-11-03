-- ========================================================
--  PMS 建表脚本 (MySQL 8.0)
--  字符集 utf8mb4，金额 DECIMAL(12,2) 元
-- ========================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 系统基础 ----------
DROP TABLE IF EXISTS hotel;
CREATE TABLE hotel (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  code            VARCHAR(32)  NOT NULL,
  address         VARCHAR(255),
  phone           VARCHAR(32),
  default_currency VARCHAR(8) DEFAULT 'CNY',
  service_fee_rate DECIMAL(5,4) DEFAULT 0,
  default_deposit DECIMAL(12,2) DEFAULT 500,
  checkout_time   TIME DEFAULT '12:00:00',
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS department;
CREATE TABLE department (
  id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name  VARCHAR(50) NOT NULL,
  code  VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS staff;
CREATE TABLE staff (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id      BIGINT NOT NULL,
  department_id BIGINT,
  username      VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  real_name     VARCHAR(50),
  phone         VARCHAR(32),
  status        TINYINT DEFAULT 1 COMMENT '1启用0停用',
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS role;
CREATE TABLE role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS permission;
CREATE TABLE permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(32),
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS role_permission;
CREATE TABLE role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS staff_role;
CREATE TABLE staff_role (
  staff_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (staff_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT,
  module VARCHAR(32),
  action VARCHAR(64),
  target_type VARCHAR(32),
  target_id BIGINT,
  detail JSON,
  ip VARCHAR(45),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 房型/房号 ----------
DROP TABLE IF EXISTS room_type;
CREATE TABLE room_type (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(50) NOT NULL,
  max_occupancy INT DEFAULT 2,
  bed_type VARCHAR(16),
  area DECIMAL(6,2),
  base_rate DECIMAL(12,2) NOT NULL,
  rack_rate DECIMAL(12,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS room;
CREATE TABLE room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  number VARCHAR(16) NOT NULL,
  floor INT,
  rent_status VARCHAR(8) DEFAULT 'OPEN' COMMENT 'OPEN打开/CLOSED关闭/ASSIGNED已排房(在住)',
  clean_status VARCHAR(8) DEFAULT 'CLEAN' COMMENT 'CLEAN净房/DIRTY脏房',
  remark VARCHAR(255),
  deleted_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_hotel_number (hotel_id, number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS room_status_log;
CREATE TABLE room_status_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  from_status VARCHAR(16),
  to_status VARCHAR(16),
  staff_id BIGINT,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 宾客 ----------
DROP TABLE IF EXISTS guest;
CREATE TABLE guest (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(32),
  gender TINYINT DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_hotel_phone (hotel_id, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 渠道 ----------
DROP TABLE IF EXISTS channel;
CREATE TABLE channel (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(32),
  is_ota TINYINT DEFAULT 0,
  icon VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 预订 ----------
DROP TABLE IF EXISTS reservation;
CREATE TABLE reservation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  res_no VARCHAR(32) NOT NULL UNIQUE,
  guest_id BIGINT,
  guest_name VARCHAR(50),
  guest_phone VARCHAR(32),
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  nights INT NOT NULL,
  channel_id BIGINT,
  status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/CHECKED_IN/NO_SHOW/CANCELLED/CHECKED_OUT',
  booker_id BIGINT,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_hotel_status (hotel_id, status),
  KEY idx_checkin (check_in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS reservation_room;
CREATE TABLE reservation_room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  room_count INT NOT NULL,
  rate DECIMAL(12,2) NOT NULL,
  assigned_room_id BIGINT NULL,
  KEY idx_res (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 在住/接待 ----------
DROP TABLE IF EXISTS stay;
CREATE TABLE stay (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  reservation_id BIGINT,
  guest_id BIGINT,
  guest_name VARCHAR(50),
  guest_phone VARCHAR(32),
  room_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  rate DECIMAL(12,2) NOT NULL,
  check_in_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  check_out_at TIMESTAMP NULL,
  status VARCHAR(16) DEFAULT 'IN' COMMENT 'IN/OUT',
  type VARCHAR(8) DEFAULT 'STAY' COMMENT 'STAY普通入住/HOURLY钟点房',
  hours INT COMMENT '钟点房时长(小时)',
  operator_id BIGINT,
  KEY idx_hotel_status (hotel_id, status),
  KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS stay_extension;
CREATE TABLE stay_extension (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stay_id BIGINT NOT NULL,
  extend_to_date DATE NOT NULL,
  remark VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS room_transfer;
CREATE TABLE room_transfer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stay_id BIGINT NOT NULL,
  from_room_id BIGINT,
  to_room_id BIGINT,
  reason VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 账务 ----------
DROP TABLE IF EXISTS folio;
CREATE TABLE folio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  stay_id BIGINT,
  reservation_id BIGINT,
  guest_id BIGINT,
  balance DECIMAL(12,2) DEFAULT 0 COMMENT '正=应收',
  status VARCHAR(16) DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_stay (stay_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS folio_item;
CREATE TABLE folio_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folio_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL COMMENT 'CHARGE/DEPOSIT/PAYMENT',
  category VARCHAR(32),
  amount DECIMAL(12,2) NOT NULL COMMENT '消费+ 押金/支付-',
  ref_folio_id BIGINT NULL,
  remark VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_folio (folio_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folio_id BIGINT NOT NULL,
  method VARCHAR(16) NOT NULL COMMENT 'CASH/CARD/WECHAT/ALIPAY/AR',
  amount DECIMAL(12,2) NOT NULL,
  operator_id BIGINT,
  paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 房务 ----------
DROP TABLE IF EXISTS housekeeping_task;
CREATE TABLE housekeeping_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  type VARCHAR(16) COMMENT 'DAILY/CHECKOUT_CLEAN',
  status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/DONE',
  assignee_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS maintenance;
CREATE TABLE maintenance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  issue VARCHAR(255),
  status VARCHAR(16) DEFAULT 'OPEN' COMMENT 'OPEN/FIXED',
  reporter_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fixed_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 房间封房（按日期范围关闭） ----------
DROP TABLE IF EXISTS room_block;
CREATE TABLE room_block (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(255),
  status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE封房中/RELEASED已释放',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  released_at TIMESTAMP NULL,
  INDEX idx_room_dates (room_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 夜审 ----------
DROP TABLE IF EXISTS night_audit;
CREATE TABLE night_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  audit_date DATE NOT NULL,
  checkins_count INT,
  checkouts_count INT,
  revenue DECIMAL(12,2),
  audited_by BIGINT,
  status VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 接口日志 ----------
DROP TABLE IF EXISTS interface_log;
CREATE TABLE interface_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel VARCHAR(32),
  direction VARCHAR(8),
  payload JSON,
  status VARCHAR(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS channel_listing;
CREATE TABLE channel_listing (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  ota_account_id BIGINT NOT NULL COMMENT '所属OTA账号',
  platform VARCHAR(32) NOT NULL COMMENT '平台: CTRIP/MEITUAN/DOUYIN',
  channel_listing_id VARCHAR(128) NOT NULL COMMENT '渠道侧房源ID',
  channel_listing_name VARCHAR(255) COMMENT '渠道侧房源名称',
  target_type VARCHAR(16) NOT NULL COMMENT '关联类型: ROOM(房间)/ROOM_TYPE(房型)',
  room_id BIGINT COMMENT '关联的房间ID(target_type=ROOM时)',
  room_type_id BIGINT COMMENT '关联的房型ID(target_type=ROOM_TYPE时)',
  inventory_type VARCHAR(16) DEFAULT 'INDEPENDENT' COMMENT '库存类型: INDEPENDENT独立房源/MULTI_STOCK多库存',
  status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE已关联/INACTIVE已解除',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_ota (hotel_id, ota_account_id),
  INDEX idx_channel_listing (platform, channel_listing_id),
  UNIQUE KEY uk_ota_listing (ota_account_id, channel_listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道房源关联表';

DROP TABLE IF EXISTS conflict_order;
CREATE TABLE conflict_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  ota_account_id BIGINT COMMENT 'OTA账号ID',
  platform VARCHAR(32) COMMENT '平台',
  channel_order_id VARCHAR(128) COMMENT '渠道侧订单号',
  guest_name VARCHAR(50),
  guest_phone VARCHAR(32),
  check_in_date DATE,
  check_out_date DATE,
  nights INT,
  room_id BIGINT,
  room_type_id BIGINT,
  conflict_type VARCHAR(32) NOT NULL COMMENT 'SYNC_DELAY/OVERLAP/STATUS_MISMATCH/MULTI_STOCK_PUZZLE',
  conflict_detail VARCHAR(500) COMMENT '冲突描述',
  status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING待处理/RESOLVED已解决/CANCELLED已取消',
  resolution VARCHAR(500) COMMENT '处理结果',
  handler_id BIGINT COMMENT '处理人',
  handled_at TIMESTAMP NULL COMMENT '处理时间',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_status (hotel_id, status),
  INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冲突订单盒子';

DROP TABLE IF EXISTS ota_account;
CREATE TABLE ota_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL COMMENT 'OTA平台: CTRIP携程/MEITUAN美团/DOUYIN抖音',
  account_name VARCHAR(128) NOT NULL COMMENT 'OTA登录账号',
  password VARCHAR(255) NOT NULL COMMENT 'OTA登录密码(加密存储)',
  merchant_id VARCHAR(128) COMMENT '商家ID/门店ID',
  app_key VARCHAR(255) COMMENT '开放平台AppKey',
  app_secret VARCHAR(255) COMMENT '开放平台AppSecret',
  session_token VARCHAR(1024) DEFAULT NULL COMMENT '加密登录态凭证(session/cookie/token)',
  session_expires_at TIMESTAMP NULL COMMENT '凭证过期时间',
  status VARCHAR(16) DEFAULT 'DISCONNECTED' COMMENT 'DISCONNECTED未连接/CONNECTED已登录/OFFLINE掉线/ERROR连接异常',
  offline_at TIMESTAMP NULL COMMENT '最近掉线时间',
  offline_reason VARCHAR(255) DEFAULT NULL COMMENT '掉线原因',
  last_error VARCHAR(500) COMMENT '最近错误信息',
  last_sync_at TIMESTAMP NULL COMMENT '最近一次同步时间',
  auto_reconnect TINYINT DEFAULT 1 COMMENT '开启自动重连:1是/0否',
  retry_count INT DEFAULT 0 COMMENT '当前退避重试次数',
  next_retry_at TIMESTAMP NULL COMMENT '下次自动重试时间',
  keepalive_interval INT DEFAULT 300 COMMENT '保活轮询间隔(秒),默认5分钟',
  server_config VARCHAR(1024) DEFAULT NULL COMMENT '风控配置JSON(UA/设备指纹/出口IP)',
  enabled TINYINT DEFAULT 1 COMMENT '1启用/0停用',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_platform (hotel_id, platform),
  UNIQUE KEY uk_hotel_platform_account (hotel_id, platform, account_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA直连账号(登录态管理)';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 初始数据 ----------
INSERT INTO hotel (id, name, code, default_deposit, checkout_time)
VALUES (1, '示例酒店', 'DEMO', 500, '12:00:00');

-- 默认管理员 admin/admin(占位，生产请用 bcrypt 替换)
INSERT INTO staff (hotel_id, username, password_hash, real_name, status)
VALUES (1, 'admin', 'bcrypt-placeholder', '管理员', 1);

INSERT INTO channel (name, code, is_ota) VALUES
('线下散客', 'WALKIN', 0),
('携程', 'CTRIP', 1),
('美团', 'MEITUAN', 1),
('飞猪', 'FLIGGY', 1),
('抖音', 'DOUYIN', 1),
('高德', 'AMAP', 1),
('美团民宿', 'MEITUAN_HOMESTAY', 1),
('途家民宿', 'TUJIA', 1),
('其他', 'OTHER', 0);

INSERT INTO room_type (hotel_id, code, name, max_occupancy, bed_type, area, base_rate, rack_rate) VALUES
(1, 'STD', '标准间', 2, '双床', 25.00, 280.00, 380.00),
(1, 'DLB', '大床房', 2, '大床', 28.00, 320.00, 420.00);

INSERT INTO room (hotel_id, room_type_id, number, floor, rent_status, clean_status) VALUES
(1, 1, '101', 1, 'OPEN', 'CLEAN'),
(1, 1, '102', 1, 'OPEN', 'CLEAN'),
(1, 2, '201', 2, 'OPEN', 'CLEAN'),
(1, 2, '202', 2, 'OPEN', 'DIRTY');
