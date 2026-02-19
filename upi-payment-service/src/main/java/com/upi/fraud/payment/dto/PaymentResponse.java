package com.upi.fraud.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long transactionId;
    private String status;
    private String receiverUpiId;
    private BigDecimal amount;
    private Integer riskScore;
    private String riskLevel;
    private String message;
    private Instant createdAt;
}
