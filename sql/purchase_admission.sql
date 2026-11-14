CREATE TABLE IF NOT EXISTS purchase_request (
  member_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  create_time DATETIME(3) NOT NULL,
  PRIMARY KEY (member_id, request_id),
  UNIQUE KEY uk_purchase_request_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

