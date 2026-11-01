package com.zapiee.orderservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Staff Order Snapshot Response",
        description = "Initial running-order snapshot sent to STAFF or OWNER WebSocket clients."
)
public class StaffOrderSnapshotResponse {

    private Long restaurantId;
    private String role;
    private List<OrderListResponse> orders;
}