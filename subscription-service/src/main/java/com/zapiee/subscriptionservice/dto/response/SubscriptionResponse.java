package com.zapiee.subscriptionservice.dto.response;

import com.zapiee.subscriptionservice.enums.Status;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private String id;
    private String razorpaySubscriptionId;
    private String razorpayKeyId;
    private PlanResponse plan;
    private SubscriptionStatus status;
    private String shortUrl;
    private Integer totalCount;
    private Integer paidCount;
    private Integer remainingCount;
    private Long startAt;
    private Long currentStart;
    private Long endAt;   // Cycle end Unix timestamp
    private Long chargeAt;     // Next charge Unix timestamp
    private Long currentEnd;   // Cycle end Unix timestamp
    private boolean isCurrentSubscription;
    private String upgradedFromPlanId;    // which plan was replaced
    private Long   upgradeStartsAt;       // epoch ms — when this plan activates
    private Boolean pendingUpgrade;       // true = waiting for old plan to expire
    private BigDecimal proratedRefundAmount; // how much was refunded (if any)
    private String proratedRefundId;
    private String replacedSubscriptionId;
    private String notes;

    // Human-readable derived fields (set in toSubscriptionResponse):
    private String upgradeStartsAtFormatted;   // "March 30, 2026"
    private String currentEndFormatted;        // "March 30, 2026"
    private Timestamp createdAt;
}