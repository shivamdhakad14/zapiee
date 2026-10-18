package com.zapiee.api_gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallBackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("User service is unavailable - Circuit breaker activated");
        return createFallbackResponse("User Service is temporarily unavailable");
    }

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("Auth service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Auth Service is temporarily unavailable");
    }

    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> menuServiceFallback() {
        log.warn("Menu service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Menu Service is temporarily unavailable");
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Order service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Order Service is temporarily unavailable");
    }

    @RequestMapping("/restaurant")
    public ResponseEntity<Map<String, Object>> restaurantServiceFallback() {
        log.warn("Restaurant service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Restaurant Service is temporarily unavailable");
    }

    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Payment Service is temporarily unavailable");
    }

    @GetMapping("/staff")
    public ResponseEntity<Map<String, Object>> staffServiceFallback() {
        log.warn("Staff service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Staff Service is temporarily unavailable");
    }

    @GetMapping("/notification")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("Notification service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Notification Service is temporarily unavailable");
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("Service is unavailable - Circuit breaker activated");
        return createFallbackResponse("Service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("suggestion", "Please try again later or contact support if the issue persists");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
