package com.upi.fraud.admin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class FraudServiceClient {

    private final RestTemplate restTemplate;
    private final String fraudServiceUrl;

    public FraudServiceClient(RestTemplate restTemplate,
                              @Value("${fraud.service.url:http://localhost:8083}") String fraudServiceUrl) {
        this.restTemplate = restTemplate;
        this.fraudServiceUrl = fraudServiceUrl;
    }

    public List<Map<String, Object>> getFraudLogs(Long userId, int page, int size) {
        String url = fraudServiceUrl + "/api/fraud/logs?page=" + page + "&size=" + size;
        if (userId != null) url += "&userId=" + userId;
        return restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}
