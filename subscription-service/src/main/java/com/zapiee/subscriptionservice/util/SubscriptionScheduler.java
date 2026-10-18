package com.zapiee.subscriptionservice.util;

import com.zapiee.subscriptionservice.client.RestaurantClient;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import com.zapiee.subscriptionservice.model.Subscription;
import com.zapiee.subscriptionservice.repository.SubscriptionRepository;
import com.zapiee.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final RestaurantClient restaurantClient;

    /**
     * Every hour: pull every active subscription from Razorpay and re-sync.
     * Safety net in case webhooks were missed.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void syncActiveSubscriptions() {
        List<Subscription> active = subscriptionRepository.findAll().stream()
                .filter(s -> s.isCurrentSubscription()
                        && s.getStatus() == SubscriptionStatus.ACTIVE)
                .toList();

        log.info("Hourly sync: {} active subscriptions", active.size());
        for (Subscription sub : active) {
            try {
                subscriptionService.syncSubscriptionStatus(sub.getRazorpaySubscriptionId());
            } catch (Exception e) {
                log.error("Sync failed for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    /**
     * Daily at 9 AM: send trial expiry reminders (7 days and 3 days before).
     * Fetches restaurant contact info from the Restaurant microservice.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendTrialExpiryReminders() {
        log.info("Trial expiry reminder job ran — wire up Restaurant service endpoint to send emails.");
    }

    /**
     * Daily at 2 AM: full nightly sync of all current subscriptions from Razorpay.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyFullSync() {
        List<Subscription> current = subscriptionRepository.findAll().stream()
                .filter(Subscription::isCurrentSubscription)
                .toList();

        log.info("Nightly sync: {} subscriptions to check", current.size());
        for (Subscription sub : current) {
            try {
                subscriptionService.syncSubscriptionStatus(sub.getRazorpaySubscriptionId());
            } catch (Exception e) {
                log.error("Nightly sync failed for {}: {}", sub.getRazorpaySubscriptionId(), e.getMessage());
            }
        }
        log.info("Nightly sync complete.");
    }
}
