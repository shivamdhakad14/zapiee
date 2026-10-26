package com.zapiee.subscriptionservice.controller;

import com.zapiee.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import com.zapiee.subscriptionservice.dto.response.SubscriptionResponse;
import com.zapiee.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/free-trail")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createFreeTrial(
            @RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
            @RequestParam String planId
            ) {
        try {
            SubscriptionResponse resp = subscriptionService.startFreeTrial(restaurantId, planId);
            return ResponseEntity.ok(ApiResponse.success(resp, "your 30days free trail successfully active"));
        } catch (Exception e) {
            log.error("free trail subscription failed for restaurant={}: {}", restaurantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to active 30 days free trail subscription: " +
                            e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(
            @RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            SubscriptionResponse resp = subscriptionService.createSubscription(restaurantId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.created(resp,
                            "Subscription created. Please complete payment at: " + resp.getShortUrl()));
        } catch (Exception e) {
            log.error("Create subscription failed for restaurant={}: {}", restaurantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to create subscription: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrent(
            @RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId) {
        return ResponseEntity.ok(
                ApiResponse.success(subscriptionService.getCurrentSubscriptionResponse(restaurantId)));
    }

    @GetMapping("/all-active")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllActiveSubscriptions()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getHistory(
            @RequestParam Long restaurantId) {
        return ResponseEntity.ok(
                ApiResponse.success(subscriptionService.getSubscriptionHistory(restaurantId)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> getStatus(@RequestParam Long restaurantId) {
        boolean active = subscriptionService.isRestaurantActive(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(active, active ? "Active" : "Inactive"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @RequestParam Long restaurantId,
            @RequestParam(defaultValue = "false") boolean immediate) {
        SubscriptionResponse resp = subscriptionService.cancelSubscription(restaurantId, immediate);
        String msg = immediate
                ? "Subscription cancelled immediately. Access revoked."
                : "Subscription will cancel at the end of the current billing cycle.";
        return ResponseEntity.ok(ApiResponse.success(resp, msg));
    }

}
