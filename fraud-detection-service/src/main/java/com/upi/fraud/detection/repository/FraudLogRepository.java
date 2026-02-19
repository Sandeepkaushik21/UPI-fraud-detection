package com.upi.fraud.detection.repository;

import com.upi.fraud.detection.entity.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {

    List<FraudLog> findByTransactionId(Long transactionId);

    List<FraudLog> findByUserIdOrderByFlaggedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);
}
