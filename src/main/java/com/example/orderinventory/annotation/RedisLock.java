package com.example.orderinventory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    String prefix() default "redis:lock:";

    String[] keys() default {};

    long waitTime() default 3;

    long leaseTime() default 30;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String message() default "请勿重复操作";
}
