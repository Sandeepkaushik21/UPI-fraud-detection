package com.upi.fraud.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationRequest {

    private Long userId;
    private String senderUpiId;
    private String receiverUpiId;
    private BigDecimal amount;
    private String ipAddress;
    private String deviceId;
    private String deviceFingerprint;
    private String previousDeviceFingerprint;
    private int recentFailedPinAttempts;
    private List<String> knownDeviceIds;
    private List<String> knownIpAddresses;
    private BigDecimal userAverageTransactionAmount;

    /** For Impossible Travel: previous transaction location and time (epoch ms). */
    private Double previousLat;
    private Double previousLon;
    private Long previousTransactionAt;
    /** Current transaction location and time (epoch ms). */
    private Double currentLat;
    private Double currentLon;
    private Long currentTransactionAt;

    /** Behavioral time-profiling: true if current time is outside user's typical active hours. */
    private Boolean unusualTime;
}
