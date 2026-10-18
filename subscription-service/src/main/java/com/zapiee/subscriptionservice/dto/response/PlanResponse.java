package com.zapiee.subscriptionservice.dto.response;

import com.zapiee.subscriptionservice.dto.PlanFeatures;
import com.zapiee.subscriptionservice.enums.PlanInterval;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanResponse {
    private String id;
    private String displayName;
    private String description;
    private BigDecimal amountMonthly;
    private String currency;
    private PlanInterval interval;
    private int trialDays;

    // Features
    private PlanFeatures planFeatures;
}