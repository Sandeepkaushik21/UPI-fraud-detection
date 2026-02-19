package com.upi.fraud.detection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fraud_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "fraud_reason")
    private String fraudReason;

    @Column(name = "risk_points")
    private Integer riskPoints;

    @Column(name = "total_risk_score")
    private Integer totalRiskScore;

    @Column(name = "flagged_at")
    private Instant flaggedAt;

    @PrePersist
    protected void onCreate() {
        flaggedAt = Instant.now();
    }
}
