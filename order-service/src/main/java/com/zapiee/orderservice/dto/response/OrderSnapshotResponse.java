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
@Schema(
        name = "Order Snapshot Response",
        description = "Current snapshot of an order sent through the Order Service WebSocket."
)
public class OrderSnapshotResponse {


    private Long restaurantId;
    private String orderId;
    private Long tableId;
    private Long tableNumber;
    private String sessionToken;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "INR";
    private boolean payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<OrderItemResponse> items;
}

