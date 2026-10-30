-- =====================================================================
-- 迁移：新增 room_block 表（房间按日期范围封房/关闭）
-- 适用：已有旧库（无 room_block 表）
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-room-block.sql
-- 注：MySQL 8 支持 CREATE TABLE IF NOT EXISTS，幂等可重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS room_block (
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
