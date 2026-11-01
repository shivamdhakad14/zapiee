package com.zapiee.orderservice.controller;


import com.zapiee.orderservice.dto.ApiResponse;
import com.zapiee.orderservice.dto.response.OrderSnapshotResponse;
import com.zapiee.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    public final OrderService orderService;

    @GetMapping("/check-order")
    public ResponseEntity<ApiResponse<OrderSnapshotResponse>> checkOrderOnTable(@RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
                                                                                  @RequestParam Long tableNumber){
        log.info("request arrived for categories by restaurant : {}", restaurantId);
        OrderSnapshotResponse response = orderService.checkRunningOrder(restaurantId,tableNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
