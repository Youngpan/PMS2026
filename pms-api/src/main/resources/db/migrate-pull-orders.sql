-- 迁移：ota_account 增量水位线 + 拉取暂存表
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-pull-orders.sql

ALTER TABLE ota_account ADD COLUMN page_watermark VARCHAR(50) DEFAULT NULL COMMENT '增量水位线(fetch_watermark)，存储最大订单时间作为下界';

-- 订单拉取暂存表（幂等+后续落单）
DROP TABLE IF EXISTS channel_order;
CREATE TABLE channel_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id BIGINT NOT NULL,
  ota_account_id BIGINT NOT NULL COMMENT '来源OTA账号',
  platform VARCHAR(32) NOT NULL COMMENT '平台: CTRIP/MEITUAN/DOUYIN',
  -- 幂等唯一键
  channel_order_id VARCHAR(128) NOT NULL COMMENT '渠道侧订单号',
  source_type VARCHAR(32) NOT NULL COMMENT '订单来源类型: Ebooking/Ebooking',
  -- 基本信息
  guest_name VARCHAR(100) COMMENT '客人姓名(脱敏)',
  guest_phone VARCHAR(32) COMMENT '客人电话(脱敏)',
  check_in_date DATE COMMENT '入住日期',
  check_out_date DATE COMMENT '离店日期',
  nights INT COMMENT '入住晚数',
  persons INT COMMENT '入住人数',
  room_type_name VARCHAR(100) COMMENT '渠道房型名称',
  room_count INT DEFAULT 1 COMMENT '预订房间数',
  total_amount DECIMAL(12,2) COMMENT '订单总额',
  currency VARCHAR(8) DEFAULT 'CNY',
  -- 渠道状态
  channel_status VARCHAR(32) COMMENT '渠道侧订单状态: Receipt/CheckIn/Canceled',
  -- 快照
  raw_json JSON COMMENT '原始接口JSON快照',
  -- 处理状态
  process_status VARCHAR(20) DEFAULT 'RAW' COMMENT 'RAW未处理/PROCESSED已落单/DUPLICATE重复/FAILED处理失败',
  processed_at TIMESTAMP NULL COMMENT '落单时间',
  local_reservation_id BIGINT COMMENT '落单后的百居易预订ID',
  error_msg VARCHAR(500) COMMENT '处理失败原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_source (channel_order_id, source_type),
  INDEX idx_account_status (ota_account_id, process_status),
  INDEX idx_hotel_created (hotel_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道订单拉取暂存表';
