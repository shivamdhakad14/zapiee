package com.zapiee.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RunningOrderResponse {
    private boolean success;
    private String message;
    private OrderSnapshotResponse order;
    private Long restaurantId;
    private Long tableNumber;
}