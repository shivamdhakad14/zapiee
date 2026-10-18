package com.zapiee.subscriptionservice.model;

import com.zapiee.subscriptionservice.dto.PlanFeatures;
import com.zapiee.subscriptionservice.enums.PlanInterval;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "plans")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Plan {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String razorpayPlanId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private Integer amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    @Builder.Default
    private PlanInterval billingInterval = PlanInterval.MONTHLY;

    @Column(name = "billing_interval_count", nullable = false)
    @Builder.Default
    private Integer billingIntervalCount = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer trialDays = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private PlanFeatures features;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "plans", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "plans", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SubscriptionHistory> subscriptionHistories = new ArrayList<>();
}
