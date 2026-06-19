package com.example.orderinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String operationType;

    private String orderNo;

    private Long productId;

    private Long userId;

    private Integer quantity;

    private String operatorNo;

    private String operatorName;

    private String ip;

    private String deviceFingerprint;

    private Integer riskScore;

    private Integer riskFlag;

    private String beforeStatus;

    private String afterStatus;

    private String remark;

    private LocalDateTime occurTime;

    private String source;
}
