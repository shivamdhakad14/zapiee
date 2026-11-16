package com.zapiee.paymentservice.client;

import com.zapiee.paymentservice.dto.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {

    @GetMapping("/api/v1/orders/{restaurantId}")
    Optional<OrderResponse> getOrderByIdAndRestaurantId(@PathVariable Long restaurantId, @RequestParam Long orderId);

    @PostMapping("/api/orders/payment/order/{orderId}")
    String paymentSuccess(@PathVariable Long orderId);
}
