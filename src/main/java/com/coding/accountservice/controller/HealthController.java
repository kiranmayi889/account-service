package com.coding.accountservice.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        HealthComponent health = healthEndpoint.health();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("service", "account-service");
        response.put("details", health);

        return ResponseEntity.ok(response);
    }
}