package com.example.orderinventory.service;

import com.example.orderinventory.entity.Inventory;
import com.example.orderinventory.entity.InventoryLog;

public interface InventoryService {

    boolean deductInventory(Long productId, Integer quantity, String orderNo);

    boolean rollbackInventory(Long productId, Integer quantity, String orderNo);

    boolean confirmSale(Long productId, Integer quantity, String orderNo);

    Inventory getInventoryByProductId(Long productId);

    void addInventoryLog(InventoryLog log);
}
