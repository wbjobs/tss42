package com.example.orderinventory.service;

import com.example.orderinventory.dto.InventoryChangeEvent;

public interface ElasticsearchService {

    void saveInventoryChangeAsync(InventoryChangeEvent event);
}
