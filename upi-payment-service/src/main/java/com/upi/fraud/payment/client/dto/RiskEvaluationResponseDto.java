package com.upi.fraud.payment.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResponseDto {

    private int totalRiskScore;
    private String riskLevel;
    private String decision;
    private List<TriggeredRuleDto> triggeredRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggeredRuleDto {
        private String ruleName;
        private String reason;
        private int points;
    }
}
