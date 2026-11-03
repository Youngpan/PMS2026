-- =====================================================================
-- 迁移：channel_product 渠道房源产品表 + ota_platform_config 平台配置表
-- 适用：已有旧库
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-channel-product.sql
-- =====================================================================

-- === 渠道房源产品表 ===
DROP TABLE IF EXISTS channel_product;
CREATE TABLE channel_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  ota_account_id BIGINT NOT NULL COMMENT '所属OTA账号',
  platform VARCHAR(32) NOT NULL COMMENT '平台: CTRIP/MEITUAN/DOUYIN',

  -- 基础信息
  channel_listing_id VARCHAR(128) NOT NULL COMMENT '渠道侧房源ID',
  listing_name VARCHAR(255) COMMENT '渠道房源名称',
  target_type VARCHAR(16) NOT NULL COMMENT '关联类型: ROOM(房间)/ROOM_TYPE(房型)',
  room_id BIGINT COMMENT '关联的房间ID(target_type=ROOM时)',
  room_type_id BIGINT COMMENT '关联的房型ID(target_type=ROOM_TYPE时)',
  inventory_type VARCHAR(16) DEFAULT 'INDEPENDENT' COMMENT '库存类型: INDEPENDENT/MULTI_STOCK',

  -- 房源状态
  product_status VARCHAR(16) DEFAULT 'ON_SALE' COMMENT '在售ON_SALE/停售OFF_SALE',
  is_direct TINYINT DEFAULT 1 COMMENT '是否直连房: 1是/0否',

  -- 价格与发票
  breakfast TINYINT DEFAULT 0 COMMENT '含早: 1是/0否',
  breakfast_fee DECIMAL(10,2) DEFAULT 0 COMMENT '早餐费(元/份)',
  invoice_type VARCHAR(32) DEFAULT 'DEFAULT' COMMENT '发票类型: DEFAULT普通发票/SPECIAL专用发票',
  invoice_tax_rate DECIMAL(5,2) DEFAULT 0 COMMENT '发票税点(%)',

  -- 入住与退改
  checkin_method VARCHAR(32) DEFAULT 'FRONT_DESK' COMMENT '入住方式: FRONT_DESK前台/SELF自助/CODE二维码',
  payment_method VARCHAR(32) DEFAULT 'ONLINE' COMMENT '房费支付方式: ONLINE线上/FRONT前台/CREDIT卡到付',
  guarantee_policy VARCHAR(255) COMMENT '担保政策',
  cancel_policy VARCHAR(255) COMMENT '取消政策',

  -- 容量与设施
  ns INT DEFAULT 1 COMMENT 'ns(不确定含义，预留)',
  nr INT DEFAULT 1 COMMENT 'nr(不确定含义，预留)',
  capacity INT DEFAULT 2 COMMENT '容纳人数',
  extra_bed TINYINT DEFAULT 0 COMMENT '加床: 1可加/0不可加',
  extra_person TINYINT DEFAULT 0 COMMENT '加人: 1可加/0不可加',
  child_policy VARCHAR(255) COMMENT '儿童政策',
  pet_policy VARCHAR(255) COMMENT '宠物政策',

  -- 客房配置
  bed_type VARCHAR(32) DEFAULT '大床' COMMENT '床型: 大床/双床/单人床',
  floor VARCHAR(16) COMMENT '楼层',
  `window` VARCHAR(32) COMMENT '窗户: 有窗/无窗/部分有窗',
  area DECIMAL(8,2) COMMENT '面积(㎡)',

  -- 其他
  status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_hotel_ota (hotel_id, ota_account_id),
  INDEX idx_platform_listing (platform, channel_listing_id),
  UNIQUE KEY uk_ota_listing (ota_account_id, channel_listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道房源产品表';

-- === OTA平台配置表 ===
DROP TABLE IF EXISTS ota_platform_config;
CREATE TABLE ota_platform_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  platform VARCHAR(32) NOT NULL COMMENT '平台代码: CTRIP/MEITUAN/DOUYIN',
  config_name VARCHAR(100) NOT NULL COMMENT '配置项名称',
  config_value TEXT COMMENT '配置项值(JSON)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_platform_config (platform, config_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA平台配置表';

-- 默认配置：携程/美团/抖音账号绑定字段规范
INSERT INTO ota_platform_config (platform, config_name, config_value) VALUES
('CTRIP', 'account_schema', '{"fields":[{"key":"accountName","label":"账号","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}'),
('MEITUAN', 'account_schema', '{"fields":[{"key":"accountName","label":"登录账号","type":"text","required":true},{"key":"password","label":"登录密码","type":"password","required":true},{"key":"merchantId","label":"商家ID","type":"text","required":true}]}'),
('DOUYIN', 'account_schema', '{"fields":[{"key":"accountName","label":"抖音号","type":"text","required":true},{"key":"password","label":"密码","type":"password","required":true}]}'),
('CTRIP', 'product_fields', '{"extraFields":[{"key":"breakfast","label":"含早","type":"switch"},{"key":"capacity","label":"容纳人数","type":"number"}]}');
