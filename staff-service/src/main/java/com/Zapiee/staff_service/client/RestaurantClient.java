package com.Zapiee.staff_service.client;


import com.Zapiee.staff_service.dto.ApiResponse;
import com.Zapiee.staff_service.dto.RestaurantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "restaurant-service",
        url = "${services.restaurant.url}"
)
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants")
    ApiResponse<RestaurantResponse> getRestaurantById(
            @RequestHeader("X-Restaurant-Id") Long restaurantId
    );
}
