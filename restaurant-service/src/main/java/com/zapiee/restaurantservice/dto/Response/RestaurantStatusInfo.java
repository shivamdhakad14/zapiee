package com.zapiee.restaurantservice.dto.Response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantStatusInfo {

    public enum Status {PENDING_REVIEW, APPROVED, REJECTED, ACTIVE, SUSPENDED, DEACTIVATED}

    private Long restaurantId;
    private Status status;
}
