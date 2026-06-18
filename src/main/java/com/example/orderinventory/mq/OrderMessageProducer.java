package com.example.orderinventory.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderMessageProducer {

    @Value("${rocketmq.consumer.order-delay-topic}")
    private String orderDelayTopic;

    @Value("${rocketmq.consumer.order-pay-topic}")
    private String orderPayTopic;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendDelayMessage(OrderDelayMessage message, int delayLevel) {
        try {
            rocketMQTemplate.syncSend(orderDelayTopic,
                    MessageBuilder.withPayload(message).build(),
                    3000,
                    delayLevel);
            log.info("发送订单延迟消息成功, orderNo: {}, delayLevel: {}", message.getOrderNo(), delayLevel);
        } catch (Exception e) {
            log.error("发送订单延迟消息失败, orderNo: {}", message.getOrderNo(), e);
            throw new RuntimeException("发送延迟消息失败", e);
        }
    }

    public void sendPayMessage(OrderPayMessage message) {
        try {
            rocketMQTemplate.convertAndSend(orderPayTopic, message);
            log.info("发送支付确认消息成功, orderNo: {}", message.getOrderNo());
        } catch (Exception e) {
            log.error("发送支付确认消息失败, orderNo: {}", message.getOrderNo(), e);
            throw new RuntimeException("发送支付消息失败", e);
        }
    }
}
