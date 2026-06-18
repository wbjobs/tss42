package com.example.orderinventory.mq;

import com.alibaba.fastjson.JSON;
import com.example.orderinventory.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.order-pay-topic}",
        consumerGroup = "${rocketmq.consumer.order-pay-group}"
)
public class OrderPayConsumer implements RocketMQListener<OrderPayMessage> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(OrderPayMessage message) {
        log.info("收到支付确认消息: {}", JSON.toJSONString(message));
        try {
            if (message.getPayStatus() == 1) {
                orderService.confirmPayOrder(message.getOrderNo());
                log.info("订单支付确认处理完成, orderNo: {}", message.getOrderNo());
            }
        } catch (Exception e) {
            log.error("订单支付确认处理失败, orderNo: {}", message.getOrderNo(), e);
        }
    }
}
