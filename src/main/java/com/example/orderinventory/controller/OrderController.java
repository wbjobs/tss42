package com.example.orderinventory.controller;

import com.example.orderinventory.common.Result;
import com.example.orderinventory.dto.CancelOrderRequest;
import com.example.orderinventory.dto.CreateOrderRequest;
import com.example.orderinventory.dto.PayCallbackRequest;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.mq.OrderPayMessage;
import com.example.orderinventory.mq.OrderMessageProducer;
import com.example.orderinventory.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderMessageProducer orderMessageProducer;

    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody @Validated CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return Result.success(order);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/pay/callback")
    public Result<Void> payCallback(@RequestBody @Validated PayCallbackRequest request) {
        try {
            OrderPayMessage payMessage = OrderPayMessage.builder()
                    .orderNo(request.getOrderNo())
                    .payStatus(request.getPayStatus())
                    .build();
            orderMessageProducer.sendPayMessage(payMessage);
            return Result.success();
        } catch (Exception e) {
            log.error("支付回调处理失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestBody @Validated CancelOrderRequest request) {
        try {
            boolean result = orderService.cancelOrder(request.getUserId(), request.getOrderId());
            if (result) {
                return Result.success();
            } else {
                return Result.fail("取消订单失败");
            }
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public Result<Order> getOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("查询订单失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
