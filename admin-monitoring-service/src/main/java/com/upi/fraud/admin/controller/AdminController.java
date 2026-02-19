package com.upi.fraud.admin.controller;

import com.upi.fraud.admin.client.FraudServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FraudServiceClient fraudServiceClient;

    @GetMapping("/fraud-logs")
    public ResponseEntity<List<Map<String, Object>>> getFraudLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> logs = fraudServiceClient.getFraudLogs(userId, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "service", "admin-monitoring",
                "message", "UPI Fraud Detection Admin Dashboard. Use /api/admin/fraud-logs to view flagged transactions."
        ));
    }
}
