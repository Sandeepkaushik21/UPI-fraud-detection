package com.upi.fraud.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {

    @NotBlank(message = "Receiver UPI ID is required")
    private String receiverUpiId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String deviceId;

    /** SHA-256 hash of device fingerprint (from frontend). Optional. */
    private String deviceFingerprint;
    /** Raw string to hash on backend (UA|screen|timezone|language). If set, backend hashes with SHA-256. */
    private String deviceFingerprintInput;

    private Double latitude;
    private Double longitude;

    private String idempotencyKey;
}
