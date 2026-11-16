package com.zapiee.paymentservice.client;

import com.zapiee.paymentservice.dto.response.RestaurantInformation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "RESTAURANT-SERVICE")
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    Optional<RestaurantInformation> getRestaurantById(@PathVariable Long restaurantId);

}
