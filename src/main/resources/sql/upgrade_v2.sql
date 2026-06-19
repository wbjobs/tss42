USE order_inventory;

ALTER TABLE t_order ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER update_time;
ALTER TABLE t_order ADD INDEX idx_status_create_time (status, create_time);
