package com.upi.fraud.payment.client.dto;

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
public class RiskEvaluationRequestDto {

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

    private Double previousLat;
    private Double previousLon;
    private Long previousTransactionAt;
    private Double currentLat;
    private Double currentLon;
    private Long currentTransactionAt;

    private Boolean unusualTime;
}
