package com.upi.fraud.payment.repository;

import com.upi.fraud.payment.entity.UpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UpiTransactionRepository extends JpaRepository<UpiTransaction, Long> {

    List<UpiTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    List<UpiTransaction> findByUserIdAndCreatedAtAfter(Long userId, Instant after);

    Optional<UpiTransaction> findByIdempotencyKey(String idempotencyKey);
}
