-- =====================================================================
-- 迁移：channel 表扩充到 9 渠道 + 预留 icon 列
-- 适用：已有旧库（channel 仅 3 行 散客/携程/美团，无 icon 列）
-- 执行：mysql -h127.0.0.1 -uroot -proot pms < migrate-channels.sql
-- 注意：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 守卫 PREPARE。
--       保持已有 id 稳定（1=线下散客原散客,2=携程,3=美团），新增 4-9。
-- =====================================================================

-- 1. 守卫加 icon 列
SET @has_icon = (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'channel' AND COLUMN_NAME = 'icon');
SET @add_icon = IF(@has_icon = 0,
  'ALTER TABLE channel ADD COLUMN icon VARCHAR(64)',
  'SELECT ''icon 列已存在，跳过'' AS msg');
PREPARE stmt FROM @add_icon; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 原 散客(WALKIN) 改名 线下散客（id=1 不变，既有 reservation.channel_id=1 仍有效）
UPDATE channel SET name = '线下散客' WHERE code = 'WALKIN' AND name = '散客';

-- 3. 按 code 不存在守卫 INSERT 6 个新渠道（飞猪/抖音/高德/美团民宿/途家民宿/其他）
SET @has_fliggy = (SELECT COUNT(*) FROM channel WHERE code = 'FLIGGY');
SET @ins_fliggy = IF(@has_fliggy = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''飞猪'', ''FLIGGY'', 1)',
  'SELECT 1');
PREPARE stmt FROM @ins_fliggy; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_douyin = (SELECT COUNT(*) FROM channel WHERE code = 'DOUYIN');
SET @ins_douyin = IF(@has_douyin = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''抖音'', ''DOUYIN'', 1)',
  'SELECT 1');
PREPARE stmt FROM @ins_douyin; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_amap = (SELECT COUNT(*) FROM channel WHERE code = 'AMAP');
SET @ins_amap = IF(@has_amap = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''高德'', ''AMAP'', 1)',
  'SELECT 1');
PREPARE stmt FROM @ins_amap; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_mtms = (SELECT COUNT(*) FROM channel WHERE code = 'MEITUAN_HOMESTAY');
SET @ins_mtms = IF(@has_mtms = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''美团民宿'', ''MEITUAN_HOMESTAY'', 1)',
  'SELECT 1');
PREPARE stmt FROM @ins_mtms; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_tujia = (SELECT COUNT(*) FROM channel WHERE code = 'TUJIA');
SET @ins_tujia = IF(@has_tujia = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''途家民宿'', ''TUJIA'', 1)',
  'SELECT 1');
PREPARE stmt FROM @ins_tujia; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_other = (SELECT COUNT(*) FROM channel WHERE code = 'OTHER');
SET @ins_other = IF(@has_other = 0,
  'INSERT INTO channel (name, code, is_ota) VALUES (''其他'', ''OTHER'', 0)',
  'SELECT 1');
PREPARE stmt FROM @ins_other; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 校验
SELECT id, name, code, is_ota, icon FROM channel ORDER BY id;
