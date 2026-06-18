package com.example.orderinventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
