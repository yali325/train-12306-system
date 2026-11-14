-- 手工执行一次；当前项目没有自动数据库迁移框架。
-- MySQL 8.0 InnoDB，供订单创建事件及后续车票事件共用。
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    lease_until DATETIME(3) NULL,
    claim_token VARCHAR(64) NULL,
    sent_time DATETIME(3) NULL,
    last_error VARCHAR(512) NULL,
    create_time DATETIME(3) NOT NULL,
    update_time DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_type_order (event_type, order_id),
    KEY idx_outbox_ready (status, next_retry_at, id),
    KEY idx_outbox_lease (status, lease_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
