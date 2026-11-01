package com.zapiee.restaurantservice.enums;

public enum SubscriptionStatus {
    CREATED,        // Just registered, not paid yet
    AUTHENTICATED,  // Card saved, awaiting first charge
    ACTIVE,         // Paid and active
    PENDING,        // Awaiting payment
    HALTED,         // Recurring charge failed (grace period)
    CANCELLED,      // Cancelled by owner
    COMPLETED,      // All billing cycles done
    EXPIRED,        // Expired without renewal
    PAUSED
}
