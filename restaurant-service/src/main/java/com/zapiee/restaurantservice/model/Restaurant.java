package com.zapiee.restaurantservice.model;


import com.zapiee.restaurantservice.enums.RestaurantStatus;
import com.zapiee.restaurantservice.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(
        name = "restaurants",
        indexes = {
                @Index(name = "idx_restaurant_owner", columnList = "ownerId"),
                @Index(name = "idx_restaurant_email", columnList = "businessEmail"),
                @Index(name = "idx_restaurant_contact", columnList = "businessContact")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false,length = 100)
    private String businessType;

    @Column(nullable = false,length = 150)
    private String businessName;

    @Column(nullable = false,length = 100)
    private String ownerName;

    @Column(nullable = false,unique = true,length = 20)
    private String businessContact;

    @Column(nullable = false,unique = true,length = 150)
    private String businessEmail;

    @Column(nullable = false)
    private String foodCategory;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Enumerated(EnumType.STRING)
    private RestaurantStatus restaurantStatus = RestaurantStatus.PENDING_REVIEW;

    private String activePlanId;
    private String planName;
    private String activeSubscriptionId;
    private String razorpayCustomerId;
    private String razorpaySubscriptionId;
    private String lastPaymentId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.PENDING;
    private Boolean isSubscriptionActive = false;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private LocalDateTime subscriptionCancelledAt;
    private Long subscriptionEndEpoch;


    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean fullTime = false;

    @OneToOne(
            mappedBy = "restaurant",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Address address;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}