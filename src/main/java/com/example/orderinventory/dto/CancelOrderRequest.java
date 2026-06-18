package com.example.orderinventory.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class CancelOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
