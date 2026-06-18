package com.example.orderinventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.orderinventory.mapper")
public class OrderInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderInventoryApplication.class, args);
    }
}
