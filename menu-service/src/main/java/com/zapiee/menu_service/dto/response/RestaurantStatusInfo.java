package com.zapiee.menu_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantStatusInfo {

    public enum Status { PENDING_REVIEW, APPROVED, REJECTED, ACTIVE, SUSPENDED, DEACTIVATED }

    private Long restaurantId;
    private Status status;
}