-- 对已有 MySQL 8.0 数据库执行一次；新建库使用 business.sql 中的同名索引。
-- 对应按日期、车次、车厢查询座位并按车厢内座序排序。
ALTER TABLE daily_train_seat
  ADD INDEX idx_seat_train_carriage_order
  (`date`, `train_code`, `carriage_index`, `carriage_seat_index`);
