-- =====================================================================
-- 迁移：room.status 单字段 → rent_status + clean_status 双字段
-- 适用：已有旧库（status 列仍为 VC/VD/OCC/OOO/BLOCK）
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-room-status.sql
-- 注意：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，本脚本为一次性迁移，
--       重复执行会因列已存在而报错。生产环境请先备份。
-- =====================================================================

-- 1. 加列
ALTER TABLE room ADD COLUMN rent_status VARCHAR(8) DEFAULT 'OPEN' COMMENT 'OPEN打开/CLOSED关闭/ASSIGNED已排房';
ALTER TABLE room ADD COLUMN clean_status VARCHAR(8) DEFAULT 'CLEAN' COMMENT 'CLEAN净房/DIRTY脏房';
ALTER TABLE room ADD COLUMN remark VARCHAR(255);

-- 2. stay 表加 type/hours + 散客姓名电话（钟点房用）
ALTER TABLE stay ADD COLUMN type VARCHAR(8) DEFAULT 'STAY' COMMENT 'STAY/HOURLY';
ALTER TABLE stay ADD COLUMN hours INT;
ALTER TABLE stay ADD COLUMN guest_name VARCHAR(50);
ALTER TABLE stay ADD COLUMN guest_phone VARCHAR(32);

-- 3. stay_extension 加 remark
ALTER TABLE stay_extension ADD COLUMN remark VARCHAR(255);

-- 4. 从旧 status 映射（仅当 status 列仍存在时）
--    VC净 → OPEN/CLEAN；VD脏 → OPEN/DIRTY；OCC在住 → ASSIGNED/CLEAN；
--    OOO维修 → CLOSED/CLEAN；BLOCK预留 → ASSIGNED/CLEAN
SET @has_status = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'room' AND COLUMN_NAME = 'status');
SET @sql = IF(@has_status = 1,
  'UPDATE room SET rent_status = CASE status WHEN ''VC'' THEN ''OPEN'' WHEN ''VD'' THEN ''OPEN'' WHEN ''OCC'' THEN ''ASSIGNED'' WHEN ''OOO'' THEN ''CLOSED'' WHEN ''BLOCK'' THEN ''ASSIGNED'' ELSE ''OPEN'' END,
                  clean_status = CASE status WHEN ''VC'' THEN ''CLEAN'' WHEN ''VD'' THEN ''DIRTY'' WHEN ''OCC'' THEN ''CLEAN'' WHEN ''OOO'' THEN ''CLEAN'' WHEN ''BLOCK'' THEN ''CLEAN'' ELSE ''CLEAN'' END',
  'SELECT ''status 列已不存在，跳过映射'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 删除旧 status 列
SET @drop = IF(@has_status = 1,
  'ALTER TABLE room DROP COLUMN status',
  'SELECT ''status 列已不存在'' AS msg');
PREPARE stmt FROM @drop; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 重建房间状态索引（原 idx 指向已删的 status 列）
SET @has_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'room' AND INDEX_NAME = 'idx_room_status');
SET @dropidx = IF(@has_idx = 1, 'ALTER TABLE room DROP INDEX idx_room_status', 'SELECT 1');
PREPARE stmt FROM @dropidx; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE room ADD INDEX idx_room_rent (hotel_id, rent_status);
