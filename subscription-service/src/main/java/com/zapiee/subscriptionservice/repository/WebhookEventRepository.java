package com.zapiee.subscriptionservice.repository;

import com.zapiee.subscriptionservice.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
    Optional<WebhookEvent> findByRazorpayEventId(String eventId);
    boolean existsByRazorpayEventId(String eventId);
    List<WebhookEvent> findByProcessedFalseOrderByReceivedAtAsc();
}