-- 开发库重置：清空房态+订单数据，房间恢复空净。仅用于 dev/test，生产勿用。
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE stay_extension;
TRUNCATE TABLE room_transfer;
TRUNCATE TABLE stay;
TRUNCATE TABLE folio_item;
TRUNCATE TABLE folio;
TRUNCATE TABLE payment;
TRUNCATE TABLE reservation_room;
TRUNCATE TABLE reservation;
TRUNCATE TABLE room_block;
TRUNCATE TABLE housekeeping_task;
TRUNCATE TABLE maintenance;
TRUNCATE TABLE night_audit;
TRUNCATE TABLE guest;
TRUNCATE TABLE room_status_log;
UPDATE room SET rent_status='OPEN', clean_status='CLEAN', remark=NULL;
SET FOREIGN_KEY_CHECKS=1;
