package com.upi.fraud.detection.controller;

import com.upi.fraud.detection.dto.FraudLogRequest;
import com.upi.fraud.detection.dto.RiskEvaluationRequest;
import com.upi.fraud.detection.dto.RiskEvaluationResponse;
import com.upi.fraud.detection.service.FraudDetectionService;
import com.upi.fraud.detection.service.RiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;
    private final RiskScoringService riskScoringService;

    @PostMapping("/evaluate")
    public ResponseEntity<RiskEvaluationResponse> evaluateRisk(@RequestBody RiskEvaluationRequest request,
                                                               @RequestParam(defaultValue = "0") int recentTxCount,
                                                               @RequestParam(defaultValue = "false") boolean newDevice,
                                                               @RequestParam(defaultValue = "false") boolean newIp) {
        RiskEvaluationResponse response = fraudDetectionService.evaluateRisk(request, recentTxCount, newDevice, newIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/log")
    public ResponseEntity<Void> logFraud(@RequestBody FraudLogRequest request) {
        if (request.getTriggeredRules() != null && !request.getTriggeredRules().isEmpty()) {
            riskScoringService.logFraud(request.getTransactionId(), request.getUserId(),
                    request.getTriggeredRules(), request.getTotalRiskScore());
        }
        return ResponseEntity.ok().build();
    }
}
