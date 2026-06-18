package com.example.orderinventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
