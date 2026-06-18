package com.example.orderinventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.orderinventory.annotation.RedisLock;
import com.example.orderinventory.dto.CreateOrderRequest;
import com.example.orderinventory.entity.InventoryLog;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.enums.InventoryLogTypeEnum;
import com.example.orderinventory.enums.OrderStatusEnum;
import com.example.orderinventory.mapper.OrderMapper;
import com.example.orderinventory.mq.OrderDelayMessage;
import com.example.orderinventory.mq.OrderMessageProducer;
import com.example.orderinventory.service.InventoryService;
import com.example.orderinventory.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private InventoryService inventoryService;

    @Resource
    private OrderMessageProducer orderMessageProducer;

    @Override
    @RedisLock(prefix = "order:create:lock:", keys = {"#request.userId", "#request.productId"})
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        String orderNo = generateOrderNo();

        boolean deductSuccess = inventoryService.deductInventory(
                request.getProductId(), request.getQuantity(), orderNo);
        if (!deductSuccess) {
            throw new RuntimeException("库存不足，扣减失败");
        }

        try {
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(BigDecimal.valueOf(99.99).multiply(BigDecimal.valueOf(request.getQuantity())));
            order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            InventoryLog inventoryLog = new InventoryLog();
            inventoryLog.setOrderNo(orderNo);
            inventoryLog.setProductId(request.getProductId());
            inventoryLog.setQuantity(request.getQuantity());
            inventoryLog.setType(InventoryLogTypeEnum.DEDUCT.getCode());
            inventoryLog.setRemark("预占库存");
            inventoryLog.setCreateTime(LocalDateTime.now());
            inventoryService.addInventoryLog(inventoryLog);

            OrderDelayMessage delayMessage = OrderDelayMessage.builder()
                    .orderNo(orderNo)
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
            orderMessageProducer.sendDelayMessage(delayMessage, 3);

            log.info("订单创建成功, orderNo: {}", orderNo);
            return order;
        } catch (Exception e) {
            log.error("创建订单失败，回滚库存, orderNo: {}", orderNo, e);
            inventoryService.rollbackInventory(request.getProductId(), request.getQuantity(), orderNo);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payCallback(String orderNo, Integer payStatus) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.warn("订单状态不是待支付, orderNo: {}, status: {}", orderNo, order.getStatus());
            return true;
        }

        if (payStatus == 1) {
            confirmPayOrder(orderNo);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long userId, Long orderId) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该订单");
        }

        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.warn("订单状态不是待支付，无法取消, orderId: {}, status: {}", orderId, order.getStatus());
            return true;
        }

        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .eq("status", OrderStatusEnum.PENDING_PAY.getCode())
                .set("status", OrderStatusEnum.CANCELLED.getCode())
                .set("update_time", LocalDateTime.now());
        int updateCount = orderMapper.update(null, updateWrapper);

        if (updateCount > 0) {
            boolean rollbackResult = inventoryService.rollbackInventory(
                    order.getProductId(), order.getQuantity(), order.getOrderNo());
            if (rollbackResult) {
                InventoryLog inventoryLog = new InventoryLog();
                inventoryLog.setOrderNo(order.getOrderNo());
                inventoryLog.setProductId(order.getProductId());
                inventoryLog.setQuantity(order.getQuantity());
                inventoryLog.setType(InventoryLogTypeEnum.ROLLBACK.getCode());
                inventoryLog.setRemark("用户取消订单回滚库存");
                inventoryLog.setCreateTime(LocalDateTime.now());
                inventoryService.addInventoryLog(inventoryLog);
            }
            log.info("订单取消成功, orderId: {}", orderId);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean timeoutCancelOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            log.warn("订单不存在, orderNo: {}", orderNo);
            return true;
        }

        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.info("订单已支付或已取消，无需超时取消, orderNo: {}, status: {}", orderNo, order.getStatus());
            return true;
        }

        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", OrderStatusEnum.PENDING_PAY.getCode())
                .set("status", OrderStatusEnum.TIMEOUT_CANCELLED.getCode())
                .set("update_time", LocalDateTime.now());
        int updateCount = orderMapper.update(null, updateWrapper);

        if (updateCount > 0) {
            boolean rollbackResult = inventoryService.rollbackInventory(
                    order.getProductId(), order.getQuantity(), orderNo);
            if (rollbackResult) {
                InventoryLog inventoryLog = new InventoryLog();
                inventoryLog.setOrderNo(orderNo);
                inventoryLog.setProductId(order.getProductId());
                inventoryLog.setQuantity(order.getQuantity());
                inventoryLog.setType(InventoryLogTypeEnum.ROLLBACK.getCode());
                inventoryLog.setRemark("订单超时自动回滚库存");
                inventoryLog.setCreateTime(LocalDateTime.now());
                inventoryService.addInventoryLog(inventoryLog);
            }
            log.info("订单超时取消成功, orderNo: {}", orderNo);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPayOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            log.info("订单已支付，无需重复确认, orderNo: {}", orderNo);
            return true;
        }

        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.warn("订单状态不是待支付, orderNo: {}, status: {}", orderNo, order.getStatus());
            return false;
        }

        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", OrderStatusEnum.PENDING_PAY.getCode())
                .set("status", OrderStatusEnum.PAID.getCode())
                .set("pay_time", LocalDateTime.now())
                .set("update_time", LocalDateTime.now());
        int updateCount = orderMapper.update(null, updateWrapper);

        if (updateCount > 0) {
            boolean confirmResult = inventoryService.confirmSale(
                    order.getProductId(), order.getQuantity(), orderNo);
            if (confirmResult) {
                InventoryLog inventoryLog = new InventoryLog();
                inventoryLog.setOrderNo(orderNo);
                inventoryLog.setProductId(order.getProductId());
                inventoryLog.setQuantity(order.getQuantity());
                inventoryLog.setType(InventoryLogTypeEnum.CONFIRM.getCode());
                inventoryLog.setRemark("支付确认，预占转销售");
                inventoryLog.setCreateTime(LocalDateTime.now());
                inventoryService.addInventoryLog(inventoryLog);
            }
            log.info("订单支付确认成功, orderNo: {}", orderNo);
            return true;
        }

        return false;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        return orderMapper.selectOne(wrapper);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
