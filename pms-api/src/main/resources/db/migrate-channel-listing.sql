-- =====================================================================
-- 迁移：新增渠道房源关联表 + 冲突订单表
-- 适用：已有旧库
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-channel-listing.sql
-- =====================================================================

-- 渠道房源关联表：渠道房源 ↔ 百居易房间/房型
DROP TABLE IF EXISTS channel_listing;
CREATE TABLE channel_listing (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  ota_account_id BIGINT NOT NULL COMMENT '所属OTA账号',
  platform VARCHAR(32) NOT NULL COMMENT '平台: CTRIP/MEITUAN/DOUYIN',
  -- 渠道侧房源信息
  channel_listing_id VARCHAR(128) NOT NULL COMMENT '渠道侧房源ID',
  channel_listing_name VARCHAR(255) COMMENT '渠道侧房源名称',
  -- 百居易侧关联目标
  target_type VARCHAR(16) NOT NULL COMMENT '关联类型: ROOM(房间)/ROOM_TYPE(房型)',
  room_id BIGINT COMMENT '关联的房间ID(target_type=ROOM时)',
  room_type_id BIGINT COMMENT '关联的房型ID(target_type=ROOM_TYPE时)',
  -- 独立房源库存=1，多库存房源库存>1
  inventory_type VARCHAR(16) DEFAULT 'INDEPENDENT' COMMENT '库存类型: INDEPENDENT独立房源/MULTI_STOCK多库存',
  -- 状态
  status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE已关联/INACTIVE已解除',
  operator_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_hotel_ota (hotel_id, ota_account_id),
  INDEX idx_channel_listing (platform, channel_listing_id),
  UNIQUE KEY uk_ota_listing (ota_account_id, channel_listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道房源关联表';

-- 冲突订单盒：同步延迟/房态不一致等原因导致的冲突订单暂存区
DROP TABLE IF EXISTS conflict_order;
CREATE TABLE conflict_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  -- 来源
  ota_account_id BIGINT COMMENT 'OTA账号ID',
  platform VARCHAR(32) COMMENT '平台',
  channel_order_id VARCHAR(128) COMMENT '渠道侧订单号',
  guest_name VARCHAR(50),
  guest_phone VARCHAR(32),
  check_in_date DATE,
  check_out_date DATE,
  nights INT,
  -- 关联的百居易房间/房型
  room_id BIGINT,
  room_type_id BIGINT,
  -- 冲突信息
  conflict_type VARCHAR(32) NOT NULL COMMENT '冲突类型: SYNC_DELAY/OVERLAP/STATUS_MISMATCH/MULTI_STOCK_PUZZLE',
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
