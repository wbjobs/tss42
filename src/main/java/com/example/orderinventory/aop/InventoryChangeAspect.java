package com.example.orderinventory.aop;

import com.example.orderinventory.annotation.InventoryChange;
import com.example.orderinventory.context.RequestContextHolder;
import com.example.orderinventory.dto.InventoryChangeEvent;
import com.example.orderinventory.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class InventoryChangeAspect {

    @Resource
    private ElasticsearchService elasticsearchService;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(inventoryChange)")
    public Object around(ProceedingJoinPoint joinPoint, InventoryChange inventoryChange) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = discoverer.getParameterNames(method);

        Map<String, Object> params = extractParams(args, parameterNames);

        String operationType = inventoryChange.operationType();
        Long productId = getLongParam(params, "productId");
        Integer quantity = getIntParam(params, "quantity");
        String orderNo = getStringParam(params, "orderNo");
        String operatorNo = getStringParam(params, "operatorNo");
        String operatorName = getStringParam(params, "operatorName");

        RequestContextHolder.RequestContext context = RequestContextHolder.getContext();

        Object result;
        String beforeStatus = "UNKNOWN";
        String afterStatus = "UNKNOWN";

        try {
            beforeStatus = getCurrentStatus(operationType, orderNo);
            result = joinPoint.proceed();
            afterStatus = getTargetStatus(operationType);

            if (Boolean.TRUE.equals(result)) {
                InventoryChangeEvent event = buildEvent(
                        operationType,
                        productId,
                        quantity,
                        orderNo,
                        operatorNo,
                        operatorName,
                        beforeStatus,
                        afterStatus,
                        context,
                        inventoryChange.description()
                );
                elasticsearchService.saveInventoryChangeAsync(event);
                log.info("[库存变动AOP] 事件已提交, type: {}, orderNo: {}, eventId: {}",
                        operationType, orderNo, event.getEventId());
            } else {
                log.info("[库存变动AOP] 方法执行失败或无变化，跳过ES写入, type: {}, orderNo: {}", operationType, orderNo);
            }
        } catch (Throwable throwable) {
            log.error("[库存变动AOP] 执行异常, type: {}, orderNo: {}", operationType, orderNo, throwable);
            throw throwable;
        }

        return result;
    }

    private Map<String, Object> extractParams(Object[] args, String[] parameterNames) {
        Map<String, Object> params = new HashMap<>();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (i < args.length) {
                    params.put(parameterNames[i], args[i]);
                }
            }
        }
        return params;
    }

    private Long getLongParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getStringParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        return value != null ? value.toString() : null;
    }

    private String getCurrentStatus(String operationType, String orderNo) {
        switch (operationType) {
            case "DEDUCT":
                return "AVAILABLE";
            case "ROLLBACK":
            case "MANUAL_RELEASE":
                return "RESERVED";
            case "CONFIRM":
                return "RESERVED";
            default:
                return "UNKNOWN";
        }
    }

    private String getTargetStatus(String operationType) {
        switch (operationType) {
            case "DEDUCT":
                return "RESERVED";
            case "ROLLBACK":
            case "MANUAL_RELEASE":
                return "AVAILABLE";
            case "CONFIRM":
                return "SOLD";
            default:
                return "UNKNOWN";
        }
    }

    private InventoryChangeEvent buildEvent(String operationType,
                                            Long productId,
                                            Integer quantity,
                                            String orderNo,
                                            String operatorNo,
                                            String operatorName,
                                            String beforeStatus,
                                            String afterStatus,
                                            RequestContextHolder.RequestContext context,
                                            String remark) {
        InventoryChangeEvent.InventoryChangeEventBuilder builder = InventoryChangeEvent.builder()
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .operationType(operationType)
                .orderNo(orderNo)
                .productId(productId)
                .quantity(quantity)
                .operatorNo(operatorNo)
                .operatorName(operatorName)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .remark(remark)
                .occurTime(LocalDateTime.now());

        if (context != null) {
            builder.userId(context.getUserId())
                    .ip(context.getIp())
                    .deviceFingerprint(context.getDeviceFingerprint())
                    .riskScore(context.getRiskScore())
                    .riskFlag(context.getRiskFlag());
        }

        if (operatorNo != null || operatorName != null) {
            builder.source("ADMIN");
        } else {
            builder.source("SYSTEM");
        }

        return builder.build();
    }
}
