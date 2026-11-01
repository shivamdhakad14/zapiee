package com.zapiee.menu_service.client;

import com.zapiee.menu_service.dto.response.RestaurantStatusInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}/status")
    RestaurantStatusInfo getStatus(@PathVariable Long restaurantId);
}
