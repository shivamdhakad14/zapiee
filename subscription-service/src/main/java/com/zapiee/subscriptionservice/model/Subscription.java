package com.zapiee.subscriptionservice.model;

import com.zapiee.subscriptionservice.enums.Status;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(
        name = "restaurant_subscriptions",
        indexes = {
                @Index(name = "idx_sub_rzp_id", columnList = "razorpay_subscription_id", unique = true),
                @Index(name = "idx_sub_restaurant", columnList = "restaurant_id"),
                @Index(name = " idx_sub_pending_upgrade", columnList = "is_pending_upgrade")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plans")
    )
    private Plan plans;

    @Column(name = "razorpay_subscription_id", nullable = false, unique = true)
    private String razorpaySubscriptionId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.CREATED;

    // Razorpay metadata
    private String shortUrl;

    private String razorpayPaymentId;

    private Integer totalCount;

    private Integer paidCount;

    private Integer remainingCount;

    // Razorpay timestamps
    private Long startAt;

    private Long endAt;

    private Long currentStart;

    private Long currentEnd;

    private Long chargeAt;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Boolean isTrial = false;

    @Column(name = "is_current_subscription")
    private boolean currentSubscription;

    @Column(name = "upgraded_from_plan_id")
    private String upgradedFromPlanId;

    @Column(name = "upgrade_starts_at")
    private Long upgradeStartsAt;

    @Column(name = "replaced_subscription_id")
    private String replacedSubscriptionId;

    @Column(name = "is_pending_upgrade")
    private Boolean pendingUpgrade;

    @Column(name = "prorated_refund_amount", precision = 10, scale = 2)
    private BigDecimal proratedRefundAmount;

    @Column(name = "prorated_refund_id")
    private String proratedRefundId;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;
}
