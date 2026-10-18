package com.zapiee.menu_service.client;

import com.zapiee.menu_service.dto.response.PlanFeaturesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service")
public interface SubscriptionServiceClient {

    @GetMapping("/internal/v1/subscriptions/{restaurantId}/features")
    PlanFeaturesResponse getFeatures(@PathVariable("restaurantId") Long restaurantId);
}
