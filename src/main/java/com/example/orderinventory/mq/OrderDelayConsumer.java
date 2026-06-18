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
        topic = "${rocketmq.consumer.order-delay-topic}",
        consumerGroup = "${rocketmq.consumer.order-cancel-group}"
)
public class OrderDelayConsumer implements RocketMQListener<OrderDelayMessage> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(OrderDelayMessage message) {
        log.info("收到订单延迟消息: {}", JSON.toJSONString(message));
        try {
            orderService.timeoutCancelOrder(message.getOrderNo());
            log.info("订单超时取消处理完成, orderNo: {}", message.getOrderNo());
        } catch (Exception e) {
            log.error("订单超时取消处理失败, orderNo: {}", message.getOrderNo(), e);
        }
    }
}
