package com.example.orderinventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_operator_log")
public class OperatorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operatorNo;

    private String operatorName;

    private String operationType;

    private String orderNo;

    private String productId;

    private String beforeData;

    private String afterData;

    private String remark;

    private String ip;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
