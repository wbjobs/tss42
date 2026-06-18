package com.example.orderinventory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class PayCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付状态不能为空")
    private Integer payStatus;
}
