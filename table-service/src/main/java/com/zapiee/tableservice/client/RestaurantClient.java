package com.zapiee.tableservice.client;


import com.zapiee.tableservice.dto.response.RestaurantInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient( name = "restaurant-service" )
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/{restaurantId}")
    RestaurantInfoResponse restaurantById(@PathVariable("restaurantId") Long restaurantId );

}