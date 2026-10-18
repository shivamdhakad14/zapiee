package com.zapiee.orderservice.dto.request;

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
        name = "Remove Item Request",
        description = "Request used to remove an item from an order."
)
public class RemoveItemRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be greater than zero")
    private Long id;

    @NotNull(message = "Order item ID is required")
    @Positive(message = "Order item ID must be greater than zero")
    private Long itemId;
}

