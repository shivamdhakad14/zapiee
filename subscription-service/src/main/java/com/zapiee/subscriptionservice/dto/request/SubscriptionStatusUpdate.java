package com.zapiee.subscriptionservice.dto.request;

import com.zapiee.subscriptionservice.enums.SubscriptionStatus;

@lombok.Getter @lombok.Setter @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
public class SubscriptionStatusUpdate {
    private String activeSubscriptionId;
    private String activePlanId;
    private SubscriptionStatus status;
    private Long subscriptionEndEpoch;
}
