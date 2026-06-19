package com.example.orderinventory.service;

import com.example.orderinventory.dto.RiskScoreRequest;
import com.example.orderinventory.dto.RiskScoreResult;

public interface RiskService {

    RiskScoreResult calculateRiskScore(RiskScoreRequest request);
}
