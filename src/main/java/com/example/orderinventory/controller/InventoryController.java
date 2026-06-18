package com.example.orderinventory.controller;

import com.example.orderinventory.common.Result;
import com.example.orderinventory.entity.Inventory;
import com.example.orderinventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @GetMapping("/{productId}")
    public Result<Inventory> getInventory(@PathVariable Long productId) {
        try {
            Inventory inventory = inventoryService.getInventoryByProductId(productId);
            return Result.success(inventory);
        } catch (Exception e) {
            log.error("查询库存失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
