package com.zapiee.subscriptionservice.model;

import com.zapiee.subscriptionservice.enums.Action;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(
        name = "subscriptions_history"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plans")
    )
    private Plan plans;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Action action = Action.PAUSED;

    @CreationTimestamp
    private Timestamp changed_at;
}
