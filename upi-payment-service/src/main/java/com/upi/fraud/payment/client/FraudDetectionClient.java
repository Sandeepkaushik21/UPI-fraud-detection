package com.upi.fraud.payment.client;

import com.upi.fraud.payment.client.dto.FraudLogRequestDto;
import com.upi.fraud.payment.client.dto.RiskEvaluationRequestDto;
import com.upi.fraud.payment.client.dto.RiskEvaluationResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class FraudDetectionClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String fraudServiceUrl;

    public FraudDetectionClient(@Value("${fraud.service.url:http://localhost:8083}") String fraudServiceUrl) {
        this.fraudServiceUrl = fraudServiceUrl;
    }

    public RiskEvaluationResponseDto evaluateRisk(RiskEvaluationRequestDto request, int recentTxCount,
                                                  boolean newDevice, boolean newIp) {
        String url = fraudServiceUrl + "/api/fraud/evaluate?recentTxCount=" + recentTxCount
                + "&newDevice=" + newDevice + "&newIp=" + newIp;
        return restTemplate.postForObject(url, request, RiskEvaluationResponseDto.class);
    }

    public void logFraud(Long transactionId, Long userId,
                         List<RiskEvaluationResponseDto.TriggeredRuleDto> triggeredRules, int totalRiskScore) {
        String url = fraudServiceUrl + "/api/fraud/log";
        FraudLogRequestDto body = FraudLogRequestDto.builder()
                .transactionId(transactionId)
                .userId(userId)
                .totalRiskScore(totalRiskScore)
                .triggeredRules(triggeredRules)
                .build();
        restTemplate.postForObject(url, body, Void.class);
    }
}
