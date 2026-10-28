package com.zapiee.orderservice.dto.response;

import com.zapiee.orderservice.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Order Connection Response",
        description = "Context returned or maintained for an authenticated Order Service WebSocket connection."
)
public class OrderConnectionResponse {

    private Long restaurantId;
    private Long tableNumber;
    private Long tableId;
    private String sessionToken;
    private Role role;
    // here i want to show order ifa table has no order running then no order shows
    // but if the order has still running then it show after the connection

}

