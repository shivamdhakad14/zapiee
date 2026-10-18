package com.zapiee.subscriptionservice.repository;

import com.zapiee.subscriptionservice.enums.Status;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import com.zapiee.subscriptionservice.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    Optional<Subscription> findByRestaurantIdAndCurrentSubscriptionTrue(Long restaurantId);
    List<Subscription> findByStatus(SubscriptionStatus status);
    List<Subscription> findByCurrentSubscriptionTrue();
    boolean existsByRestaurantId(Long restaurantId);
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

}
