package com.upi.fraud.detection.service;

import com.upi.fraud.detection.dto.RiskEvaluationRequest;
import com.upi.fraud.detection.dto.RiskEvaluationResponse;
import com.upi.fraud.detection.entity.FraudLog;
import com.upi.fraud.detection.repository.FraudLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final FraudLogRepository fraudLogRepository;

    @Value("${fraud.risk-points.rapid-transaction:30}")
    private int rapidTransactionPoints;
    @Value("${fraud.risk-points.amount-spike:25}")
    private int amountSpikePoints;
    @Value("${fraud.risk-points.new-device:20}")
    private int newDevicePoints;
    @Value("${fraud.risk-points.ip-change:15}")
    private int ipChangePoints;
    @Value("${fraud.risk-points.failed-pin:40}")
    private int failedPinPoints;
    @Value("${fraud.risk-points.impossible-travel:100}")
    private int impossibleTravelPoints;
    @Value("${fraud.risk-points.device-spoofing:40}")
    private int deviceSpoofingPoints;
    @Value("${fraud.risk-points.unusual-time:25}")
    private int unusualTimePoints;
    @Value("${fraud.risk-levels.low-max:30}")
    private int lowMax;
    @Value("${fraud.risk-levels.medium-max:60}")
    private int mediumMax;

    public RiskEvaluationResponse evaluate(RiskEvaluationRequest request, boolean rapidTransaction,
                                           boolean amountSpike, boolean newDevice, boolean ipChange,
                                           boolean failedPinAttempts, boolean impossibleTravel,
                                           boolean deviceSpoofing, boolean unusualTime) {
        List<RiskEvaluationResponse.TriggeredRule> triggered = new ArrayList<>();
        int total = 0;

        if (rapidTransaction) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Rapid Transactions")
                    .reason("More than 3 transactions within 60 seconds")
                    .points(rapidTransactionPoints)
                    .build());
            total += rapidTransactionPoints;
        }
        if (amountSpike) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Amount Spike")
                    .reason("Transaction amount greater than 5x user average")
                    .points(amountSpikePoints)
                    .build());
            total += amountSpikePoints;
        }
        if (newDevice) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("New Device")
                    .reason("Transaction from unregistered device")
                    .points(newDevicePoints)
                    .build());
            total += newDevicePoints;
        }
        if (ipChange) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("IP Change")
                    .reason("Transaction from new IP in short period")
                    .points(ipChangePoints)
                    .build());
            total += ipChangePoints;
        }
        if (failedPinAttempts) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Failed PIN Attempts")
                    .reason("More than 3 incorrect UPI PIN attempts")
                    .points(failedPinPoints)
                    .build());
            total += failedPinPoints;
        }
        if (impossibleTravel) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Impossible Travel")
                    .reason("Velocity attack: physical location change impossible in elapsed time")
                    .points(impossibleTravelPoints)
                    .build());
            total += impossibleTravelPoints;
        }
        if (deviceSpoofing) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Device Spoofing")
                    .reason("Same device ID but different hardware fingerprint (hash mismatch)")
                    .points(deviceSpoofingPoints)
                    .build());
            total += deviceSpoofingPoints;
        }
        if (Boolean.TRUE.equals(unusualTime)) {
            triggered.add(RiskEvaluationResponse.TriggeredRule.builder()
                    .ruleName("Unusual Time (UEBA)")
                    .reason("Transaction outside user's typical active hours")
                    .points(unusualTimePoints)
                    .build());
            total += unusualTimePoints;
        }

        String riskLevel = total <= lowMax ? "LOW" : (total <= mediumMax ? "MEDIUM" : "HIGH");
        String decision = total <= lowMax ? "APPROVE" : (total <= mediumMax ? "SUSPICIOUS" : "BLOCK");

        return RiskEvaluationResponse.builder()
                .totalRiskScore(total)
                .riskLevel(riskLevel)
                .decision(decision)
                .triggeredRules(triggered)
                .build();
    }

    public void logFraud(Long transactionId, Long userId, List<RiskEvaluationResponse.TriggeredRule> rules, int totalScore) {
        if (rules.isEmpty()) return;
        for (RiskEvaluationResponse.TriggeredRule r : rules) {
            FraudLog log = FraudLog.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .fraudReason(r.getRuleName() + ": " + r.getReason())
                    .riskPoints(r.getPoints())
                    .totalRiskScore(totalScore)
                    .build();
            fraudLogRepository.save(log);
        }
    }
}
