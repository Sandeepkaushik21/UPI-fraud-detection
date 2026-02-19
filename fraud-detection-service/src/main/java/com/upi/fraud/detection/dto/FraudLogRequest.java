package com.upi.fraud.detection.dto;

import lombok.Data;

import java.util.List;

@Data
public class FraudLogRequest {

    private Long transactionId;
    private Long userId;
    private int totalRiskScore;
    private List<RiskEvaluationResponse.TriggeredRule> triggeredRules;
}
