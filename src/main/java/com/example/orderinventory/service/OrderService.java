package com.example.orderinventory.service;

import com.example.orderinventory.dto.CreateOrderRequest;
import com.example.orderinventory.entity.Order;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);

    boolean payCallback(String orderNo, Integer payStatus);

    boolean cancelOrder(Long userId, Long orderId);

    boolean timeoutCancelOrder(String orderNo);

    boolean confirmPayOrder(String orderNo);

    Order getOrderById(Long orderId);

    Order getOrderByOrderNo(String orderNo);
}
