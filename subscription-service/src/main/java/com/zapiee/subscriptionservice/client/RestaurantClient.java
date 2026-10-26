package com.zapiee.subscriptionservice.client;

import com.zapiee.subscriptionservice.dto.request.RestaurantInformation;
import com.zapiee.subscriptionservice.dto.request.SubscriptionStatusUpdate;
import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    Optional<RestaurantInformation> getRestaurantInformation(@PathVariable Long restaurantId);

    @PutMapping("/api/v1/restaurants/{restaurantId}/subscription-status")
    ApiResponse<Void> updateSubscriptionStatus(@PathVariable Long restaurantId, @RequestBody SubscriptionStatusUpdate update);

    @PutMapping("/api/v1/restaurants/{restaurantId}/razorpay-customer")
    ApiResponse<Void> updateRazorpayCustomer(@PathVariable Long restaurantId, @RequestParam String razorpayCustomerId);
}
