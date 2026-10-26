package com.zapiee.subscriptionservice.controller;

import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import com.zapiee.subscriptionservice.dto.response.WebhookEventResponse;
import com.zapiee.subscriptionservice.handler.RazorpayWebhookHandler;
import com.zapiee.subscriptionservice.service.WebhookEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final RazorpayWebhookHandler webhookHandler;
    private final WebhookEventService webhookEventService;

    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpay(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader("X-Razorpay-Event-Id") String eventId) {

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook received without X-Razorpay-Signature header");
            return ResponseEntity.badRequest().body(Map.of("error", "missing_signature"));
        }

        boolean ok = webhookHandler.handleWebhook(payload, signature, eventId);
        return ok
                ? ResponseEntity.ok(Map.of("status", "ok"))
                : ResponseEntity.badRequest().body(Map.of("error", "invalid_signature"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEventResponse>>> getAllEvents(){
        log.info("request received for get all webhook events");
        List<WebhookEventResponse> allWebhookEvent = webhookEventService.getAllWebhookEvent();
        return ResponseEntity.ok(ApiResponse.success(allWebhookEvent));
    }
 }
