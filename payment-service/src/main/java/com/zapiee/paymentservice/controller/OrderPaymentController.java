package com.zapiee.paymentservice.controller;

import com.zapiee.paymentservice.dto.request.InitiateOrderPaymentRequest;
import com.zapiee.paymentservice.dto.request.OrderPaymentRequest;
import com.zapiee.paymentservice.dto.response.*;
import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import com.zapiee.paymentservice.service.OrderPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/order-payments")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;

    @PostMapping("/init/{restaurantId}")
    public ResponseEntity<ApiResponse<OrderPaymentInitResponse>> orderInitialize(@PathVariable Long restaurantId,
                                                                                 @Valid @RequestBody InitiateOrderPaymentRequest orderPaymentRequest){
        OrderPaymentInitResponse upiPaymentInitResponse = orderPaymentService.initiateOrder(restaurantId, orderPaymentRequest);
        return ResponseEntity.ok(ApiResponse.success(upiPaymentInitResponse));
    }

    @PostMapping("/confirm/{restaurantId}")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrderPayment(@PathVariable Long restaurantId,
                                                                          @Valid @RequestBody OrderPaymentRequest orderPaymentRequest){
        OrderResponse upiPaymentInitResponse = orderPaymentService.confirmOrderPayment(restaurantId, orderPaymentRequest);
        return ResponseEntity.ok(ApiResponse.success(upiPaymentInitResponse));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<PagedResponse<OrderPaymentResponse>> getOrderPaymentByRestId(@PathVariable Long restaurantId,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size){
        log.info("request received for get all order payment by restaurant : {}", restaurantId);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderPaymentResponse> orderPayments = orderPaymentService.
                getOrderPaymentByRestaurantId(restaurantId, pageable);

        return ResponseEntity.ok(orderPayments);
    }

    @GetMapping("/{restaurantId}/status")
    public ResponseEntity<PagedResponse<OrderPaymentResponse>> getOrderPaymentByRestIdAndStatus(@PathVariable Long restaurantId,
                                                                                       @RequestParam OrderPaymentStatus status,
                                                                                       @RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "10") int size){
        log.info("request received for get all order payment by restaurant : {} and status : {}", restaurantId, status);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderPaymentResponse> orderPayments = orderPaymentService.
                getOrderPaymentByRestaurantIdAndStatus(restaurantId,status,pageable);
        return ResponseEntity.ok(orderPayments);
    }
}
