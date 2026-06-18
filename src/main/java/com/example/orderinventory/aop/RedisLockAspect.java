package com.example.orderinventory.aop;

import com.example.orderinventory.annotation.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RedisLockAspect {

    @Resource
    private RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = discoverer.getParameterNames(method);

        String lockKey = buildLockKey(redisLock, method, args, parameterNames);
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
            if (!acquired) {
                throw new RuntimeException(redisLock.message());
            }
            return joinPoint.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildLockKey(RedisLock redisLock, Method method, Object[] args, String[] parameterNames) {
        StringBuilder keyBuilder = new StringBuilder(redisLock.prefix());
        String[] keys = redisLock.keys();

        if (keys.length == 0) {
            keyBuilder.append(method.getName());
            return keyBuilder.toString();
        }

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                keyBuilder.append(":");
            }
            String keyExpression = keys[i];
            if (keyExpression.contains("#")) {
                Object value = parser.parseExpression(keyExpression).getValue(context);
                keyBuilder.append(value != null ? value.toString() : "null");
            } else {
                keyBuilder.append(keyExpression);
            }
        }

        return keyBuilder.toString();
    }
}
