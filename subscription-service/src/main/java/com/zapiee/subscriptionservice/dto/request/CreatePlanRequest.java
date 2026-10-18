package com.zapiee.subscriptionservice.dto.request;

import com.zapiee.subscriptionservice.dto.PlanFeatures;
import com.zapiee.subscriptionservice.enums.PlanInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePlanRequest {

    @NotBlank(message = "Plan ID is required")
    private String id;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Amount must be at least 100 paise")
    private Integer amountPaise;

    @Builder.Default
    private PlanInterval interval = PlanInterval.MONTHLY;

    @Builder.Default
    @Min(value = 1, message = "Interval count must be at least 1")
    private Integer intervalCount = 1;

    @NotNull(message = "features is required")
    private PlanFeatures planFeatures;

    @Builder.Default
    @Min(value = 0, message = "Trial days cannot be negative")
    private Integer trialDays = 0;
}
