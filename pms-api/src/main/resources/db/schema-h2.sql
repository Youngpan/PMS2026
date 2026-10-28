-- H2 (MODE=MySQL) 兼容建表脚本，用于本地冒烟测试
-- 与 schema.sql 表结构等价，去除 MySQL 专有语法（JSON->LONGTEXT, ON UPDATE, COMMENT, 内联 KEY）

-- ---------- 系统基础 ----------
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS staff_role;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS hotel;

CREATE TABLE hotel (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(32) NOT NULL,
  address VARCHAR(255),
  phone VARCHAR(32),
  default_currency VARCHAR(8) DEFAULT 'CNY',
  service_fee_rate DECIMAL(5,4) DEFAULT 0,
  default_deposit DECIMAL(12,2) DEFAULT 500,
  checkout_time TIME DEFAULT '12:00:00',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE department (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE staff (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  department_id BIGINT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(50),
  phone VARCHAR(32),
  status INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(32)
);

CREATE TABLE permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(32),
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64)
);

CREATE TABLE role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE staff_role (
  staff_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (staff_id, role_id)
);

CREATE TABLE operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT,
  module VARCHAR(32),
  action VARCHAR(64),
  target_type VARCHAR(32),
  target_id BIGINT,
  detail LONGTEXT,
  ip VARCHAR(45),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 房型/房号 ----------
DROP TABLE IF EXISTS room_status_log;
DROP TABLE IF EXISTS room;
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
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  number VARCHAR(16) NOT NULL,
  floor INT,
  rent_status VARCHAR(8) DEFAULT 'OPEN',
  clean_status VARCHAR(8) DEFAULT 'CLEAN',
  remark VARCHAR(255),
  deleted_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_hotel_number UNIQUE (hotel_id, number)
);
CREATE INDEX idx_room_rent ON room (hotel_id, rent_status);
CREATE INDEX idx_room_type ON room (room_type_id);

CREATE TABLE room_status_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  from_status VARCHAR(16),
  to_status VARCHAR(16),
  staff_id BIGINT,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 宾客 ----------
DROP TABLE IF EXISTS guest;
CREATE TABLE guest (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(32),
  gender INT DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_guest_phone ON guest (hotel_id, phone);

-- ---------- 渠道 ----------
DROP TABLE IF EXISTS channel;
CREATE TABLE channel (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(32),
  is_ota INT DEFAULT 0
);

-- ---------- 预订 ----------
DROP TABLE IF EXISTS reservation_room;
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
  status VARCHAR(16) DEFAULT 'PENDING',
  booker_id BIGINT,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_res_status ON reservation (hotel_id, status);
CREATE INDEX idx_res_checkin ON reservation (check_in_date);

CREATE TABLE reservation_room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  room_count INT NOT NULL,
  rate DECIMAL(12,2) NOT NULL,
  assigned_room_id BIGINT
);
CREATE INDEX idx_resroom ON reservation_room (reservation_id);

-- ---------- 在住/接待 ----------
DROP TABLE IF EXISTS room_transfer;
DROP TABLE IF EXISTS stay_extension;
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
  status VARCHAR(16) DEFAULT 'IN',
  type VARCHAR(8) DEFAULT 'STAY',
  hours INT,
  operator_id BIGINT
);
CREATE INDEX idx_stay_status ON stay (hotel_id, status);
CREATE INDEX idx_stay_room ON stay (room_id);

CREATE TABLE stay_extension (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stay_id BIGINT NOT NULL,
  extend_to_date DATE NOT NULL,
  remark VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room_transfer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stay_id BIGINT NOT NULL,
  from_room_id BIGINT,
  to_room_id BIGINT,
  reason VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 账务 ----------
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS folio_item;
DROP TABLE IF EXISTS folio;

CREATE TABLE folio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  stay_id BIGINT,
  reservation_id BIGINT,
  guest_id BIGINT,
  balance DECIMAL(12,2) DEFAULT 0,
  status VARCHAR(16) DEFAULT 'OPEN',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_folio_stay ON folio (stay_id);

CREATE TABLE folio_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folio_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL,
  category VARCHAR(32),
  amount DECIMAL(12,2) NOT NULL,
  ref_folio_id BIGINT,
  remark VARCHAR(255),
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_folioitem ON folio_item (folio_id, created_at);

CREATE TABLE payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folio_id BIGINT NOT NULL,
  method VARCHAR(16) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  operator_id BIGINT,
  paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 房务 ----------
DROP TABLE IF EXISTS housekeeping_task;
DROP TABLE IF EXISTS maintenance;

CREATE TABLE housekeeping_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  type VARCHAR(16),
  status VARCHAR(16) DEFAULT 'PENDING',
  assignee_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL
);

CREATE TABLE maintenance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  issue VARCHAR(255),
  status VARCHAR(16) DEFAULT 'OPEN',
  reporter_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fixed_at TIMESTAMP NULL
);

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
  status VARCHAR(16) DEFAULT 'DRAFT',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 接口日志 ----------
DROP TABLE IF EXISTS interface_log;
CREATE TABLE interface_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel VARCHAR(32),
  direction VARCHAR(8),
  payload LONGTEXT,
  status VARCHAR(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- 初始数据 ----------
INSERT INTO hotel (id, name, code, default_deposit, checkout_time) VALUES (1, '示例酒店', 'DEMO', 500, '12:00');
INSERT INTO staff (hotel_id, username, password_hash, real_name, status) VALUES (1, 'admin', 'bcrypt-placeholder', '管理员', 1);
INSERT INTO channel (name, code, is_ota) VALUES
('散客', 'WALKIN', 0),
('携程', 'CTRIP', 1),
('美团', 'MEITUAN', 1);
INSERT INTO room_type (hotel_id, code, name, max_occupancy, bed_type, area, base_rate, rack_rate) VALUES
(1, 'STD', '标准间', 2, '双床', 25.00, 280.00, 380.00),
(1, 'DLB', '大床房', 2, '大床', 28.00, 320.00, 420.00);
INSERT INTO room (hotel_id, room_type_id, number, floor, rent_status, clean_status) VALUES
(1, 1, '101', 1, 'OPEN', 'CLEAN'),
(1, 1, '102', 1, 'OPEN', 'CLEAN'),
(1, 2, '201', 2, 'OPEN', 'CLEAN'),
(1, 2, '202', 2, 'OPEN', 'DIRTY');
