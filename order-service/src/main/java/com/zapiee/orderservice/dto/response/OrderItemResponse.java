package com.zapiee.orderservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Order Item Response",
        description = "Menu item information stored as part of an order."
)
public class OrderItemResponse {

    private Long itemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal lineTotal;
    private String imageUrl;
    private String currency;
}

