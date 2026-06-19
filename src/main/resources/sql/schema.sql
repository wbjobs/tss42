CREATE DATABASE IF NOT EXISTS order_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE order_inventory;

DROP TABLE IF EXISTS t_inventory;
CREATE TABLE t_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    reserved_stock INT NOT NULL DEFAULT 0 COMMENT '预占库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态 0-待支付 1-已支付 2-已取消 3-超时取消',
    risk_flag TINYINT NOT NULL DEFAULT 0 COMMENT '风险标记 0-正常 1-高风险',
    risk_score INT NOT NULL DEFAULT 0 COMMENT '风险评分 0-100',
    ip VARCHAR(64) DEFAULT NULL COMMENT '用户IP地址',
    device_fingerprint VARCHAR(128) DEFAULT NULL COMMENT '设备指纹',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id),
    KEY idx_status (status),
    KEY idx_risk_flag (risk_flag),
    KEY idx_ip (ip),
    KEY idx_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

DROP TABLE IF EXISTS t_inventory_log;
CREATE TABLE t_inventory_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '库存变动数量',
    type TINYINT NOT NULL COMMENT '类型 1-扣减(预占) 2-确认销售 3-回滚',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id),
    KEY idx_type (type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

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

INSERT INTO t_inventory (product_id, total_stock, available_stock, reserved_stock, sold_stock) VALUES
(1, 1000, 1000, 0, 0),
(2, 500, 500, 0, 0),
(3, 200, 200, 0, 0);
