package com.zapiee.mediaservice.client;

import com.zapiee.mediaservice.dto.RestaurantInformation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    Optional<RestaurantInformation> getRestaurantInformation(@PathVariable Long restaurantId);
}
