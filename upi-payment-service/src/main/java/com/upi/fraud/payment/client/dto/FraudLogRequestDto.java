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
public class FraudLogRequestDto {

    private Long transactionId;
    private Long userId;
    private int totalRiskScore;
    private List<RiskEvaluationResponseDto.TriggeredRuleDto> triggeredRules;
}
