package com.zapiee.subscriptionservice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSubscriptionRequest {

    @NotBlank(message = "Plan ID is required")
    private String planId;

    @Min(1) @Max(60)
    @Builder.Default
    private int totalCount = 12;

    // Optional
    private String notifyEmail;
    private String notifyPhone;
    private Long startAt;

    // UpgradeContext is NOT part of the HTTP request body — it's passed internally
    // by the service when upgradeSubscription() calls createSubscription().
    // It is transient (no @Column) and never serialized to JSON.
    @Transient
    @JsonIgnore
    private UpgradeContext upgradeContext;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UpgradeContext {
        private final String     oldRazorpaySubscriptionId; // the sub being replaced
        private final String     oldPlanId;                 // plan being upgraded FROM
        private final boolean    immediateUpgrade;          // true = starts now, false = pending
        private final Long       upgradeStartsAt;           // epoch seconds (null if immediate)
        private final String     proratedRefundId;          // null if no refund was issued
        private final BigDecimal proratedRefundAmount;      // null if no refund was issued
    }
}