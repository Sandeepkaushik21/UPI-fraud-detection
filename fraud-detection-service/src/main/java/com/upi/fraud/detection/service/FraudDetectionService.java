package com.upi.fraud.detection.service;

import com.upi.fraud.detection.dto.RiskEvaluationRequest;
import com.upi.fraud.detection.dto.RiskEvaluationResponse;
import com.upi.fraud.detection.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final RiskScoringService riskScoringService;

    @Value("${fraud.rules.amount-spike-multiplier:5}")
    private double amountSpikeMultiplier;
    @Value("${fraud.rules.failed-pin-threshold:3}")
    private int failedPinThreshold;
    @Value("${fraud.rules.impossible-travel-max-speed-kmh:1200}")
    private double impossibleTravelMaxSpeedKmh;

    public RiskEvaluationResponse evaluateRisk(RiskEvaluationRequest request,
                                               int recentTransactionCountInWindow,
                                               boolean isNewDevice,
                                               boolean isNewIp) {
        boolean amountSpike = isAmountSpike(request.getAmount(), request.getUserAverageTransactionAmount());
        boolean failedPin = request.getRecentFailedPinAttempts() >= failedPinThreshold;
        boolean impossibleTravel = isImpossibleTravel(request);
        boolean deviceSpoofing = isDeviceSpoofing(request);
        boolean unusualTime = Boolean.TRUE.equals(request.getUnusualTime());

        return riskScoringService.evaluate(
                request,
                recentTransactionCountInWindow >= 3,
                amountSpike,
                isNewDevice,
                isNewIp,
                failedPin,
                impossibleTravel,
                deviceSpoofing,
                unusualTime
        );
    }

    private boolean isAmountSpike(BigDecimal amount, BigDecimal userAverage) {
        if (userAverage == null || userAverage.compareTo(BigDecimal.ZERO) == 0) return false;
        BigDecimal threshold = userAverage.multiply(BigDecimal.valueOf(amountSpikeMultiplier));
        return amount.compareTo(threshold) > 0;
    }

    /**
     * Impossible Travel (Velocity / ATO): physical move impossible in elapsed time.
     * Uses Haversine distance and required speed vs max allowed (e.g. 1200 km/h).
     */
    private boolean isImpossibleTravel(RiskEvaluationRequest request) {
        Double prevLat = request.getPreviousLat();
        Double prevLon = request.getPreviousLon();
        Long prevAt = request.getPreviousTransactionAt();
        Double curLat = request.getCurrentLat();
        Double curLon = request.getCurrentLon();
        Long curAt = request.getCurrentTransactionAt();
        if (prevLat == null || prevLon == null || prevAt == null || curLat == null || curLon == null || curAt == null)
            return false;
        long elapsedSeconds = (curAt - prevAt) / 1000;
        if (elapsedSeconds <= 0) return false;
        double distanceKm = HaversineUtil.distanceKm(prevLat, prevLon, curLat, curLon);
        double requiredSpeedKmh = HaversineUtil.requiredSpeedKmh(distanceKm, elapsedSeconds);
        return requiredSpeedKmh > impossibleTravelMaxSpeedKmh;
    }

    /**
     * Device Spoofing: same deviceId but different hardware fingerprint (hash).
     */
    private boolean isDeviceSpoofing(RiskEvaluationRequest request) {
        String fingerprint = request.getDeviceFingerprint();
        String previousFingerprint = request.getPreviousDeviceFingerprint();
        if (fingerprint == null || fingerprint.isBlank() || previousFingerprint == null || previousFingerprint.isBlank())
            return false;
        return !fingerprint.equals(previousFingerprint);
    }
}
