package com.example.orderinventory.service;

import com.example.orderinventory.dto.CreateOrderRequest;
import com.example.orderinventory.entity.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);

    boolean payCallback(String orderNo, Integer payStatus);

    boolean cancelOrder(Long userId, Long orderId);

    @Deprecated
    boolean timeoutCancelOrder(String orderNo);

    boolean safeTimeoutCancelOrder(String orderNo);

    boolean confirmPayOrder(String orderNo);

    Order getOrderById(Long orderId);

    Order getOrderByOrderNo(String orderNo);

    Order getOrderByOrderNoDirectFromDb(String orderNo);

    List<Order> listTimeoutPendingOrders(int timeoutSeconds, int limit);
}
