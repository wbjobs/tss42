USE order_inventory;

ALTER TABLE t_order 
    ADD COLUMN risk_flag TINYINT NOT NULL DEFAULT 0 COMMENT '风险标记 0-正常 1-高风险' AFTER status,
    ADD COLUMN risk_score INT NOT NULL DEFAULT 0 COMMENT '风险评分 0-100' AFTER risk_flag,
    ADD COLUMN ip VARCHAR(64) DEFAULT NULL COMMENT '用户IP地址' AFTER device_fingerprint,
    ADD COLUMN device_fingerprint VARCHAR(128) DEFAULT NULL COMMENT '设备指纹' AFTER risk_score,
    ADD INDEX idx_risk_flag (risk_flag),
    ADD INDEX idx_ip (ip);

DROP TABLE IF EXISTS t_operator_log;
CREATE TABLE t_operator_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    operator_no VARCHAR(64) NOT NULL COMMENT '操作员编号',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作员姓名',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
    product_id VARCHAR(64) DEFAULT NULL COMMENT '关联商品ID',
    before_data TEXT DEFAULT NULL COMMENT '操作前数据(JSON)',
    after_data TEXT DEFAULT NULL COMMENT '操作后数据(JSON)',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    ip VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_operator_no (operator_no),
    KEY idx_operation_type (operation_type),
    KEY idx_order_no (order_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作员日志表';
