package com.zapiee.subscriptionservice.dto.request;

import com.zapiee.subscriptionservice.enums.Status;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantInformation {
    private Long restaurantId;
    private String name;
    private String email;
    private String phone;
    private Status status;
    private SubscriptionStatus subscriptionStatus;
    private String activePlanId;
    private String activeSubscriptionId;
    private String razorpayCustomerId;
    private String razorpayLinkedAccountId;
}
