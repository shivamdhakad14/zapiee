package com.zapiee.subscriptionservice.client;

import com.zapiee.subscriptionservice.dto.request.RestaurantInformation;
import com.zapiee.subscriptionservice.dto.request.SubscriptionStatusUpdate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    Optional<RestaurantInformation> getRestaurantInformation(@PathVariable Long restaurantId);

    @PutMapping("/api/restaurants/{restaurantId}/subscription-status")
    void updateSubscriptionStatus(@PathVariable Long restaurantId, @RequestBody SubscriptionStatusUpdate update);

    @PutMapping("/api/restaurants/{restaurantId}/razorpay-customer")
    void updateRazorpayCustomerId(
            @PathVariable Long restaurantId,
            @RequestParam String razorpayCustomerId
    );
}
