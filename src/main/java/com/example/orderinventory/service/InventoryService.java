package com.example.orderinventory.service;

import com.example.orderinventory.annotation.InventoryChange;
import com.example.orderinventory.entity.Inventory;
import com.example.orderinventory.entity.InventoryLog;

public interface InventoryService {

    @InventoryChange(operationType = "DEDUCT", description = "扣减库存")
    boolean deductInventory(Long productId, Integer quantity, String orderNo);

    @InventoryChange(operationType = "DEDUCT", description = "扣减库存(自定义过期时间)")
    boolean deductInventory(Long productId, Integer quantity, String orderNo, int expireSeconds);

    @InventoryChange(operationType = "ROLLBACK", description = "回滚库存")
    boolean rollbackInventory(Long productId, Integer quantity, String orderNo);

    @InventoryChange(operationType = "CONFIRM", description = "确认销售")
    boolean confirmSale(Long productId, Integer quantity, String orderNo);

    @InventoryChange(operationType = "MANUAL_RELEASE", description = "运营手动释放库存")
    boolean manualReleaseInventory(Long productId, Integer quantity, String orderNo, String operatorNo, String operatorName);

    Inventory getInventoryByProductId(Long productId);

    void addInventoryLog(InventoryLog log);
}
