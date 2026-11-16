package com.zapiee.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long tableNumber;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";
}
