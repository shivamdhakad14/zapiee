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
        name = "Order List Response",
        description = "Order summary returned when listing orders."
)
public class OrderListResponse {

    private Long id;
    private String orderId;
    private Long restaurantId;
    private Long tableId;
    private Long tableNumber;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "INR";
    private boolean payment;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<OrderItemResponse> items;
}
