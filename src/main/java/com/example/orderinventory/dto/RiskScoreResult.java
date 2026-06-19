package com.example.orderinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer score;

    private Boolean highRisk;

    private String riskLevel;

    private String riskReason;

    private Integer reserveSeconds;
}
