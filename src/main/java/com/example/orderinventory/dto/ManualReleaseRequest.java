package com.example.orderinventory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class ManualReleaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "操作员编号不能为空")
    private String operatorNo;

    @NotBlank(message = "操作员姓名不能为空")
    private String operatorName;

    private String remark;
}
