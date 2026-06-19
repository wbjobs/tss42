package com.example.orderinventory.job;

import com.example.orderinventory.entity.Order;
import com.example.orderinventory.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OrderTimeoutCancelJob {

    private static final String PENDING_CHECK_SET_KEY = "order:pending:timeout:check";
    private static final int BATCH_SIZE = 100;

    @Value("${order.timeout:900}")
    private int orderTimeoutSeconds;

    @Resource
    private OrderService orderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @XxlJob("orderTimeoutCancelJobHandler")
    public void orderTimeoutCancelJobHandler() {
        log.info("[XXL-JOB] 订单超时回滚任务开始执行...");
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;

        try {
            int processedFromSet = processPendingCheckSet();
            totalProcessed += processedFromSet;

            List<Order> timeoutOrders;
            do {
                timeoutOrders = orderService.listTimeoutPendingOrders(orderTimeoutSeconds, BATCH_SIZE);
                if (timeoutOrders == null || timeoutOrders.isEmpty()) {
                    break;
                }
                log.info("[XXL-JOB] 从DB扫描到 {} 笔超时待支付订单", timeoutOrders.size());

                for (Order order : timeoutOrders) {
                    totalProcessed++;
                    try {
                        boolean result = orderService.safeTimeoutCancelOrder(order.getOrderNo());
                        if (result) {
                            totalSuccess++;
                        } else {
                            totalFailed++;
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        log.error("[XXL-JOB] 处理订单超时回滚异常, orderNo: {}", order.getOrderNo(), e);
                    }
                }
            } while (timeoutOrders.size() >= BATCH_SIZE);

        } catch (Exception e) {
            log.error("[XXL-JOB] 订单超时回滚任务执行异常", e);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            log.info("[XXL-JOB] 订单超时回滚任务执行完成, 总处理: {}, 成功: {}, 失败: {}, 耗时: {}ms",
                    totalProcessed, totalSuccess, totalFailed, cost);
        }
    }

    private int processPendingCheckSet() {
        int processed = 0;
        try {
            Set<String> orderNos = stringRedisTemplate.opsForSet().members(PENDING_CHECK_SET_KEY);
            if (orderNos == null || orderNos.isEmpty()) {
                return 0;
            }
            log.info("[XXL-JOB] Redis待检查集合中有 {} 个订单", orderNos.size());

            List<String> toRemove = new ArrayList<>();
            for (String orderNo : orderNos) {
                processed++;
                try {
                    boolean result = orderService.safeTimeoutCancelOrder(orderNo);
                    if (result) {
                        toRemove.add(orderNo);
                    }
                } catch (Exception e) {
                    log.error("[XXL-JOB] 处理Redis待检查订单异常, orderNo: {}", orderNo, e);
                }
            }

            if (!toRemove.isEmpty()) {
                stringRedisTemplate.opsForSet().remove(PENDING_CHECK_SET_KEY, toRemove.toArray());
                log.info("[XXL-JOB] 已从Redis待检查集合中移除 {} 个已处理订单", toRemove.size());
            }
        } catch (Exception e) {
            log.error("[XXL-JOB] 处理Redis待检查集合异常", e);
        }
        return processed;
    }
}
