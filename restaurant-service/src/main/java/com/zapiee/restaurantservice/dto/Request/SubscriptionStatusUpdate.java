package com.zapiee.restaurantservice.dto.Request;

import com.zapiee.restaurantservice.enums.SubscriptionStatus;

@lombok.Getter @lombok.Setter @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
public class SubscriptionStatusUpdate {
    private String activeSubscriptionId;
    private String activePlanId;
    private SubscriptionStatus status;
    private Long subscriptionEndEpoch;
}