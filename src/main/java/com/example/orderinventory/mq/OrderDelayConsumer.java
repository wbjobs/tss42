package com.example.orderinventory.mq;

import com.alibaba.fastjson.JSON;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.enums.OrderStatusEnum;
import com.example.orderinventory.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.order-delay-topic}",
        consumerGroup = "${rocketmq.consumer.order-cancel-group}"
)
public class OrderDelayConsumer implements RocketMQListener<OrderDelayMessage> {

    private static final String PENDING_CHECK_SET_KEY = "order:pending:timeout:check";

    @Resource
    private OrderService orderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(OrderDelayMessage message) {
        log.info("[DelayConsumer] 收到订单延迟消息: {}", JSON.toJSONString(message));
        String orderNo = message.getOrderNo();
        try {
            Order order = orderService.getOrderByOrderNoDirectFromDb(orderNo);
            if (order == null) {
                log.warn("[DelayConsumer] 订单不存在，跳过, orderNo: {}", orderNo);
                return;
            }

            if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
                log.info("[DelayConsumer] 订单状态已变更({})，无需回滚, orderNo: {}",
                        order.getStatus(), orderNo);
                return;
            }

            log.info("[DelayConsumer] 将订单加入待扫描检查集合, orderNo: {}, status: {}, createTime: {}",
                    orderNo, order.getStatus(), order.getCreateTime());
            stringRedisTemplate.opsForSet().add(PENDING_CHECK_SET_KEY, orderNo);

        } catch (Exception e) {
            log.error("[DelayConsumer] 延迟消息处理失败，将订单加入待扫描集合, orderNo: {}", orderNo, e);
            try {
                stringRedisTemplate.opsForSet().add(PENDING_CHECK_SET_KEY, orderNo);
            } catch (Exception ex) {
                log.error("[DelayConsumer] 写入待扫描集合也失败, orderNo: {}", orderNo, ex);
            }
        }
    }
}
