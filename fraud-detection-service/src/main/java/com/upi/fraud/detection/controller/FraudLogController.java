package com.upi.fraud.detection.controller;

import com.upi.fraud.detection.entity.FraudLog;
import com.upi.fraud.detection.repository.FraudLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudLogController {

    private final FraudLogRepository fraudLogRepository;

    @GetMapping("/logs")
    public ResponseEntity<List<FraudLog>> getFraudLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<FraudLog> logs = userId != null
                ? fraudLogRepository.findByUserIdOrderByFlaggedAtDesc(userId, PageRequest.of(page, size))
                : fraudLogRepository.findAll(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(logs);
    }
}
