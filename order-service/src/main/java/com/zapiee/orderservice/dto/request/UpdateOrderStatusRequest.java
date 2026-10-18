package com.zapiee.orderservice.dto.request;

import com.zapiee.orderservice.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Update Order Status Request",
        description = "Request used by authorized staff or owner to change an order status."
)
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be greater than zero")
    private Long id;

    @NotNull(message = "Order status is required")
    private OrderStatus status;

}

