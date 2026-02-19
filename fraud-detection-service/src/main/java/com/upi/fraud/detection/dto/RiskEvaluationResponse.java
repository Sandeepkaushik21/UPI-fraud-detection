package com.upi.fraud.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResponse {

    private int totalRiskScore;
    private String riskLevel;
    private String decision;
    private List<TriggeredRule> triggeredRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggeredRule {
        private String ruleName;
        private String reason;
        private int points;
    }
}
