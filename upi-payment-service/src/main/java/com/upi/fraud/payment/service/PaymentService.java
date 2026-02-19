package com.upi.fraud.payment.service;

import com.upi.fraud.payment.client.FraudDetectionClient;
import com.upi.fraud.payment.client.dto.RiskEvaluationRequestDto;
import com.upi.fraud.payment.client.dto.RiskEvaluationResponseDto;
import com.upi.fraud.payment.dto.InitiatePaymentRequest;
import com.upi.fraud.payment.dto.PaymentResponse;
import com.upi.fraud.payment.entity.Account;
import com.upi.fraud.payment.entity.UpiTransaction;
import com.upi.fraud.payment.repository.AccountRepository;
import com.upi.fraud.payment.repository.UpiTransactionRepository;
import com.upi.fraud.payment.util.FingerprintUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final UpiTransactionRepository transactionRepository;
    private final FraudDetectionClient fraudDetectionClient;

    @Value("${fraud.service.url:http://localhost:8083}")
    private String fraudServiceUrl;

    public Account getOrCreateAccount(Long userId, String upiId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Account acc = Account.builder()
                            .userId(userId)
                            .upiId(upiId)
                            .balance(new BigDecimal("10000.00"))
                            .build();
                    return accountRepository.save(acc);
                });
    }

    @Transactional
    public PaymentResponse initiatePayment(Long userId, String upiId, InitiatePaymentRequest request, String ipAddress) {
        Account account = getOrCreateAccount(userId, upiId);
        if (request.getIdempotencyKey() != null) {
            transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .ifPresent(tx -> {
                        throw new IllegalStateException("Duplicate transaction. Idempotency key already used.");
                    });
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            return PaymentResponse.builder()
                    .status(UpiTransaction.TransactionStatus.FAILED.name())
                    .message("Insufficient balance")
                    .build();
        }

        Instant windowStart = Instant.now().minusSeconds(60);
        List<UpiTransaction> recentTxs = transactionRepository.findByUserIdAndCreatedAtAfter(userId, windowStart);
        int recentCount = recentTxs.size();

        List<String> knownDevices = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                .stream()
                .map(UpiTransaction::getDeviceId)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        List<String> knownIps = recentTxs.stream()
                .map(UpiTransaction::getIpAddress)
                .filter(ip -> ip != null && !ip.isBlank())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        BigDecimal avgAmount = BigDecimal.ZERO;
        if (!recentTxs.isEmpty()) {
            avgAmount = recentTxs.stream()
                    .map(UpiTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(recentTxs.size()), 2, RoundingMode.HALF_UP);
        }

        boolean newDevice = request.getDeviceId() != null && !request.getDeviceId().isBlank()
                && !knownDevices.contains(request.getDeviceId());
        boolean newIp = ipAddress != null && !ipAddress.isBlank() && !knownIps.contains(ipAddress);

        String deviceFingerprint = request.getDeviceFingerprint();
        if (deviceFingerprint == null && request.getDeviceFingerprintInput() != null && !request.getDeviceFingerprintInput().isBlank()) {
            deviceFingerprint = FingerprintUtil.sha256Hex(request.getDeviceFingerprintInput());
        }

        Instant now = Instant.now();
        long currentEpochMs = now.toEpochMilli();

        List<UpiTransaction> lastTxs = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1));
        UpiTransaction lastTx = lastTxs.isEmpty() ? null : lastTxs.get(0);
        Double previousLat = lastTx != null ? lastTx.getLatitude() : null;
        Double previousLon = lastTx != null ? lastTx.getLongitude() : null;
        Long previousTransactionAt = lastTx != null && lastTx.getCreatedAt() != null ? lastTx.getCreatedAt().toEpochMilli() : null;
        String previousDeviceFingerprint = null;
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank() && lastTx != null
                && request.getDeviceId().equals(lastTx.getDeviceId()) && lastTx.getDeviceFingerprint() != null) {
            previousDeviceFingerprint = lastTx.getDeviceFingerprint();
        }

        boolean unusualTime = computeUnusualTime(userId, now);

        RiskEvaluationRequestDto fraudRequest = RiskEvaluationRequestDto.builder()
                .userId(userId)
                .senderUpiId(upiId)
                .receiverUpiId(request.getReceiverUpiId())
                .amount(request.getAmount())
                .ipAddress(ipAddress)
                .deviceId(request.getDeviceId())
                .deviceFingerprint(deviceFingerprint)
                .previousDeviceFingerprint(previousDeviceFingerprint)
                .recentFailedPinAttempts(0)
                .knownDeviceIds(knownDevices)
                .knownIpAddresses(knownIps)
                .userAverageTransactionAmount(avgAmount)
                .previousLat(previousLat)
                .previousLon(previousLon)
                .previousTransactionAt(previousTransactionAt)
                .currentLat(request.getLatitude())
                .currentLon(request.getLongitude())
                .currentTransactionAt(currentEpochMs)
                .unusualTime(unusualTime)
                .build();

        RiskEvaluationResponseDto fraudResponse;
        try {
            fraudResponse = fraudDetectionClient.evaluateRisk(fraudRequest, recentCount, newDevice, newIp);
        } catch (Exception e) {
            fraudResponse = RiskEvaluationResponseDto.builder()
                    .totalRiskScore(0)
                    .riskLevel("LOW")
                    .decision("APPROVE")
                    .triggeredRules(List.of())
                    .build();
        }

        UpiTransaction tx = UpiTransaction.builder()
                .userId(userId)
                .receiverUpiId(request.getReceiverUpiId())
                .amount(request.getAmount())
                .status(UpiTransaction.TransactionStatus.PENDING)
                .ipAddress(ipAddress)
                .deviceId(request.getDeviceId())
                .deviceFingerprint(deviceFingerprint)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .riskScore(fraudResponse.getTotalRiskScore())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        tx = transactionRepository.save(tx);

        UpiTransaction.TransactionStatus status;
        String message;
        switch (fraudResponse.getDecision()) {
            case "BLOCK":
                status = UpiTransaction.TransactionStatus.BLOCKED;
                message = "Transaction blocked due to high fraud risk (score: " + fraudResponse.getTotalRiskScore() + ")";
                break;
            case "SUSPICIOUS":
                status = UpiTransaction.TransactionStatus.SUSPICIOUS;
                account.setBalance(account.getBalance().subtract(request.getAmount()));
                accountRepository.save(account);
                message = "Transaction marked suspicious. Completed with monitoring.";
                break;
            default:
                status = UpiTransaction.TransactionStatus.SUCCESS;
                account.setBalance(account.getBalance().subtract(request.getAmount()));
                accountRepository.save(account);
                message = "Payment successful.";
                break;
        }

        tx.setStatus(status);
        transactionRepository.save(tx);

        if ((status == UpiTransaction.TransactionStatus.BLOCKED || status == UpiTransaction.TransactionStatus.SUSPICIOUS)
                && fraudResponse.getTriggeredRules() != null && !fraudResponse.getTriggeredRules().isEmpty()) {
            fraudDetectionClient.logFraud(tx.getId(), userId, fraudResponse.getTriggeredRules(), fraudResponse.getTotalRiskScore());
        }

        return PaymentResponse.builder()
                .transactionId(tx.getId())
                .status(status.name())
                .receiverUpiId(request.getReceiverUpiId())
                .amount(request.getAmount())
                .riskScore(fraudResponse.getTotalRiskScore())
                .riskLevel(fraudResponse.getRiskLevel())
                .message(message)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    public List<PaymentResponse> getTransactionHistory(Long userId, int page, int size) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(t -> PaymentResponse.builder()
                        .transactionId(t.getId())
                        .status(t.getStatus().name())
                        .receiverUpiId(t.getReceiverUpiId())
                        .amount(t.getAmount())
                        .riskScore(t.getRiskScore())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public BigDecimal getBalance(Long userId, String upiId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> getOrCreateAccount(userId, upiId != null ? upiId : "user@" + userId))
                .getBalance();
    }

    /**
     * Behavioral time-profiling (UEBA): true if current time is outside user's typical active hours.
     * Uses last 20 transactions; active window = [minHour - 2, maxHour + 2]; need at least 5 txs.
     */
    private boolean computeUnusualTime(Long userId, Instant currentTime) {
        List<UpiTransaction> history = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20));
        if (history.size() < 5) return false;
        int currentHour = currentTime.atZone(ZoneOffset.UTC).getHour();
        int minHour = history.stream()
                .map(UpiTransaction::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .mapToInt(inst -> inst.atZone(ZoneOffset.UTC).getHour())
                .min().orElse(0);
        int maxHour = history.stream()
                .map(UpiTransaction::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .mapToInt(inst -> inst.atZone(ZoneOffset.UTC).getHour())
                .max().orElse(23);
        int buffer = 2;
        return currentHour < minHour - buffer || currentHour > maxHour + buffer;
    }
}
