package com.zapiee.orderservice.dto.response;

import com.zapiee.orderservice.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed order response")
public class OrderResponse {

    private String orderId;
    private Long restaurantId;
    private Long tableId;
    private Long tableNumber;
    private String sessionToken;

    private OrderStatus status;

    private BigDecimal subtotal;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @Schema(example = "INR")
    private String currency;

    private boolean payment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private List<OrderItemResponse> items;
}