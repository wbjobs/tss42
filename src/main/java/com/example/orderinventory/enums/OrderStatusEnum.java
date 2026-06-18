package com.example.orderinventory.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {

    PENDING_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    TIMEOUT_CANCELLED(3, "超时取消");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
