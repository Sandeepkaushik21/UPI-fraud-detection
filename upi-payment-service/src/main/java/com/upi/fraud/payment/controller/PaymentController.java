package com.upi.fraud.payment.controller;

import com.upi.fraud.payment.dto.InitiatePaymentRequest;
import com.upi.fraud.payment.dto.PaymentResponse;
import com.upi.fraud.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upi")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "upi-payment-service"));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request,
                                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String upiId = (String) httpRequest.getAttribute("upiId");
        String ip = httpRequest.getRemoteAddr();
        if (ip == null || ip.isEmpty()) ip = httpRequest.getHeader("X-Forwarded-For");
        PaymentResponse response = paymentService.initiatePayment(userId, upiId, request, ip);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String upiId = (String) httpRequest.getAttribute("upiId");
        BigDecimal balance = paymentService.getBalance(userId, upiId);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getHistory(HttpServletRequest httpRequest,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<PaymentResponse> history = paymentService.getTransactionHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }
}
