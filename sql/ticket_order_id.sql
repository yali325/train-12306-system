-- 与现有历史车票兼容：旧记录的 order_id 保持 NULL。
ALTER TABLE ticket ADD COLUMN order_id BIGINT NULL AFTER id;
ALTER TABLE ticket ADD UNIQUE KEY uk_ticket_order_passenger (order_id, passenger_id);
