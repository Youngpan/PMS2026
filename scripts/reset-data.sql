-- PMS 冒烟/演示数据重置脚本。
-- 清空业务（交易型）表，保留种子（hotel/department/staff/role/permission/room_type/room/channel）。
-- 重置房态到种子状态。使冒烟测试在持久化 MySQL 上可重复运行。
-- 用法： mysql -uroot -proot pms < scripts/reset-data.sql

SET FOREIGN_KEY_CHECKS = 0;

-- 账务
TRUNCATE TABLE folio_item;
TRUNCATE TABLE payment;
TRUNCATE TABLE folio;
-- 在住/接待
TRUNCATE TABLE stay_extension;
TRUNCATE TABLE room_transfer;
TRUNCATE TABLE stay;
-- 预订
TRUNCATE TABLE reservation_room;
TRUNCATE TABLE reservation;
-- 房务
TRUNCATE TABLE housekeeping_task;
TRUNCATE TABLE maintenance;
TRUNCATE TABLE room_status_log;
-- 夜审 / 日志 / 宾客
TRUNCATE TABLE night_audit;
TRUNCATE TABLE operation_log;
TRUNCATE TABLE interface_log;
TRUNCATE TABLE guest;

SET FOREIGN_KEY_CHECKS = 1;

-- 房态重置为种子状态：101/102/201 = 打开+净房，202 = 打开+脏房
UPDATE room SET rent_status = 'OPEN', clean_status = 'CLEAN', remark = NULL, updated_at = CURRENT_TIMESTAMP WHERE number IN ('101','102','201');
UPDATE room SET rent_status = 'OPEN', clean_status = 'DIRTY', remark = NULL, updated_at = CURRENT_TIMESTAMP WHERE number = '202';
