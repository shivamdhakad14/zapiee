package com.zapiee.mediaservice.dto;

import com.zapiee.mediaservice.enums.Status;
import com.zapiee.mediaservice.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
