package com.example.orderinventory.enums;

import lombok.Getter;

@Getter
public enum InventoryLogTypeEnum {

    DEDUCT(1, "扣减库存（预占）"),
    CONFIRM(2, "确认销售"),
    ROLLBACK(3, "回滚库存");

    private final Integer code;
    private final String desc;

    InventoryLogTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
