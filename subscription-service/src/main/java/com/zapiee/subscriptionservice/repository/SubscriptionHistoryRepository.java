package com.zapiee.subscriptionservice.repository;

import com.zapiee.subscriptionservice.model.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {
    List<SubscriptionHistory> findByRestaurantId(Long restaurantId);
}
