package com.zapiee.subscriptionservice.service;

import com.razorpay.Customer;
import com.razorpay.RazorpayException;
import com.zapiee.subscriptionservice.client.RestaurantClient;
import com.zapiee.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.zapiee.subscriptionservice.dto.request.RestaurantInformation;
import com.zapiee.subscriptionservice.dto.request.SubscriptionStatusUpdate;
import com.zapiee.subscriptionservice.dto.response.SubscriptionResponse;
import com.zapiee.subscriptionservice.enums.Status;
import com.zapiee.subscriptionservice.enums.SubscriptionStatus;
import com.zapiee.subscriptionservice.exception.ResourceNotFoundException;
import com.zapiee.subscriptionservice.exception.SubscriptionException;
import com.zapiee.subscriptionservice.model.Plan;
import com.zapiee.subscriptionservice.repository.SubscriptionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.razorpay.RazorpayClient;
import com.zapiee.subscriptionservice.model.Subscription;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final RazorpayClient razorpayClient;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final RestaurantClient restaurantClient;

    private static final int TRIAL_DURATION_DAYS = 30;
    private static final int STANDARD_TOTAL_COUNT = 12; // 12 monthly cycles

    @Transactional
    @CircuitBreaker(name = "razorpay", fallbackMethod = "startFreeTrialFallback")
    @Retry(name = "razorpay")
    public SubscriptionResponse startFreeTrial(Long restaurantId, String planId)
            throws RazorpayException {

        if (subscriptionRepository.existsByRestaurantId(restaurantId)) {
            throw new SubscriptionException(
                    "This restaurant is not eligible for a free trial.",
                    "TRIAL_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }

        RestaurantInformation restaurant = getRestaurant(restaurantId);
        Plan plan = planService.getPlanById(planId);

        String customerId = ensureRazorpayCustomer(restaurantId, restaurant);

        long now = Instant.now().getEpochSecond();
        long trialEndEpoch = now + (plan.getTrialDays() * 24L * 3600);

        JSONObject subData = new JSONObject()
                .put("plan_id",         plan.getRazorpayPlanId())
                .put("customer_notify", 1)
                .put("total_count",     STANDARD_TOTAL_COUNT)
                .put("quantity",        1)
                .put("customer_id",     customerId)

                .put("start_at",        trialEndEpoch)
                .put("notes", new JSONObject()
                        .put("restaurant_id",   restaurantId)
                        .put("restaurant_name", restaurant.getName())
                        .put("plan_id",         plan.getId())
                        .put("is_trial",        true));

        subData.put("notify_info", new JSONObject()
                .put("email",   restaurant.getEmail())
                .put("contact", restaurant.getPhone()));

        com.razorpay.Subscription rzpSub   = razorpayClient.subscriptions.create(subData);
        String       rzpSubId = rzpSub.get("id");
        String       shortUrl = rzpSub.get("short_url");

        log.info("Trial subscription created in Razorpay: {} for restaurant={}, starts billing at={}",
                rzpSubId, restaurantId, trialEndEpoch);

        com.zapiee.subscriptionservice.model.Subscription trialSub = subscriptionRepository.save(
                com.zapiee.subscriptionservice.model.Subscription.builder()
                        .restaurantId(restaurantId)
                        .plans(plan)
                        .razorpaySubscriptionId(rzpSubId)
                        .status(SubscriptionStatus.AUTHENTICATED)
                        .shortUrl(shortUrl)
                        .totalCount(STANDARD_TOTAL_COUNT)
                        .paidCount(0)
                        .remainingCount(STANDARD_TOTAL_COUNT)
                        .currentStart(now)
                        .currentEnd(trialEndEpoch)
                        .currentSubscription(true)
                        .isTrial(true)
                        .build()
        );

        restaurantClient.updateSubscriptionStatus(restaurantId,
                SubscriptionStatusUpdate.builder()
                        .status(SubscriptionStatus.AUTHENTICATED)
                        .activeSubscriptionId(rzpSubId)
                        .activePlanId(plan.getId())
                        .subscriptionEndEpoch(trialEndEpoch)
                        .build());

        SubscriptionResponse response = toSubscriptionResponse(trialSub, plan, restaurant.getName());
        response.setUpgradeStartsAtFormatted(formatDate(trialEndEpoch));
        return response;
    }

    @Transactional
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createSubscriptionFallback")
    @Retry(name = "razorpay")
    public SubscriptionResponse createSubscription(Long restaurantId,
                                                   CreateSubscriptionRequest req)
            throws RazorpayException {

        log.info("createSubscription | restaurant={} plan={}", restaurantId, req.getPlanId());

        RestaurantInformation restaurant = getRestaurant(restaurantId);
        Plan plan = planService.getPlanById(req.getPlanId());
        String customerId = ensureRazorpayCustomer(restaurantId, restaurant);

        JSONObject subData = new JSONObject()
                .put("plan_id",         plan.getRazorpayPlanId())
                .put("customer_notify", 1)
                .put("total_count",     req.getTotalCount())
                .put("quantity",        1)
                .put("customer_id",     customerId)
                .put("notes", new JSONObject()
                        .put("restaurant_id",   restaurantId)
                        .put("restaurant_name", restaurant.getName())
                        .put("plan_id",         plan.getId()));

        subData.put("notify_info", new JSONObject()
                .put("email",   req.getNotifyEmail()  != null ? req.getNotifyEmail()  : restaurant.getEmail())
                .put("contact", req.getNotifyPhone()  != null ? req.getNotifyPhone()  : restaurant.getPhone()));

        com.razorpay.Subscription rzpSub   = razorpayClient.subscriptions.create(subData);
        String       rzpSubId = rzpSub.get("id");
        String       shortUrl = rzpSub.get("short_url");
        log.info("Razorpay subscription created: {} for restaurant={}", rzpSubId, restaurantId);

        subscriptionRepository
                .findByRestaurantIdAndCurrentSubscriptionTrue(restaurantId)
                .ifPresent(old -> {
                    old.setCurrentSubscription(false);
                    subscriptionRepository.save(old);
                    log.debug("Demoted previous current subscription {}", old.getRazorpaySubscriptionId());
                });

        Subscription sub = subscriptionRepository.save(
                Subscription.builder()
                        .restaurantId(restaurantId)
                        .plans(plan)
                        .razorpaySubscriptionId(rzpSubId)
                        .status(SubscriptionStatus.CREATED)
                        .shortUrl(shortUrl)
                        .totalCount(req.getTotalCount())
                        .paidCount(0)
                        .remainingCount(req.getTotalCount())
                        .currentSubscription(true)
                        .isTrial(false)
                        .build()
        );

        restaurantClient.updateSubscriptionStatus(restaurantId,
                SubscriptionStatusUpdate.builder()
                        .status(SubscriptionStatus.CREATED)
                        .activeSubscriptionId(rzpSubId)
                        .activePlanId(plan.getId())
                        .build());

        return toSubscriptionResponse(sub, plan, restaurant.getName());
    }

    @Transactional
    public void syncSubscriptionStatus(String razorpaySubscriptionId) {
        try {
            com.razorpay.Subscription rzpSub    = razorpayClient.subscriptions.fetch(razorpaySubscriptionId);
            String           statusStr = rzpSub.get("status");
            SubscriptionStatus newStatus = mapRazorpayStatus(statusStr);

            subscriptionRepository
                    .findByRazorpaySubscriptionId(razorpaySubscriptionId)
                    .ifPresent(sub -> {
                        SubscriptionStatus oldStatus = sub.getStatus();

                        sub.setStatus(newStatus);
                        extractAndSetCounts(sub, rzpSub);

                        if (sub.getIsTrial() && newStatus == SubscriptionStatus.ACTIVE
                                && sub.getPaidCount() != null && sub.getPaidCount() > 0) {
                            sub.setIsTrial(false);
                            log.info("Trial converted to paid for restaurant={} (sub={})",
                                    sub.getRestaurantId(), razorpaySubscriptionId);
                        }

                        subscriptionRepository.save(sub);

                        boolean active = (newStatus == SubscriptionStatus.ACTIVE
                                || newStatus == SubscriptionStatus.AUTHENTICATED);

                        restaurantClient.updateSubscriptionStatus(sub.getRestaurantId(),
                                SubscriptionStatusUpdate.builder()
                                        .status(newStatus)
                                        .activeSubscriptionId(razorpaySubscriptionId)
                                        .activePlanId(sub.getPlans().getId())
                                        .subscriptionEndEpoch(sub.getCurrentEnd())
                                        .build());

                        log.info("Synced subscription {} → {} (active={}, isTrial={})",
                                razorpaySubscriptionId, newStatus, active, sub.getIsTrial());

                        if (oldStatus != newStatus) {
                            sendStatusChangeNotification(sub.getRestaurantId(), oldStatus, newStatus);
                        }
                    });

        } catch (RazorpayException e) {
            log.error("Failed to sync subscription {}: {}", razorpaySubscriptionId, e.getMessage());
        }
    }


    @Transactional
    public SubscriptionResponse cancelSubscription(Long restaurantId, boolean immediate) {
        RestaurantInformation      restaurant = getRestaurant(restaurantId);
        Subscription sub        = getCurrentSubscription(restaurantId);

        try {
            boolean effectiveImmediate = sub.getIsTrial() || immediate;

            JSONObject params = new JSONObject()
                    .put("cancel_at_cycle_end", effectiveImmediate ? 0 : 1);
            razorpayClient.subscriptions.cancel(sub.getRazorpaySubscriptionId(), params);

            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setCurrentSubscription(false);
            subscriptionRepository.save(sub);

            if (effectiveImmediate) {
                restaurantClient.updateSubscriptionStatus(restaurantId,
                        SubscriptionStatusUpdate.builder()
                                .status(SubscriptionStatus.CANCELLED)
                                .activeSubscriptionId(null)
                                .activePlanId(restaurant.getActivePlanId())
                                .build());
            }

            return toSubscriptionResponse(sub, sub.getPlans(), restaurant.getName());

        } catch (RazorpayException e) {
            log.error("Cancel subscription failed for restaurant={}: {}", restaurantId, e.getMessage());
            throw new SubscriptionException("Cancel failed: " + e.getMessage(), "RAZORPAY_ERROR");
        }
    }

    public List<SubscriptionResponse> getSubscriptionHistory(Long restaurantId) {
        RestaurantInformation restaurant = getRestaurant(restaurantId);
        return subscriptionRepository
                .findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(sub -> toSubscriptionResponse(sub, sub.getPlans(), restaurant.getName()))
                .toList();
    }

    public SubscriptionResponse getCurrentSubscriptionResponse(Long restaurantId) {
        RestaurantInformation      restaurant = getRestaurant(restaurantId);
        Subscription sub        = getCurrentSubscription(restaurantId);
        return toSubscriptionResponse(sub, sub.getPlans(), restaurant.getName());
    }

    public boolean isRestaurantActive(Long restaurantId) {
        try {
            RestaurantInformation restaurant = getRestaurant(restaurantId);
            return restaurant.getStatus() == Status.ACTIVE;
        } catch (Exception e) {
            log.warn("Could not verify active status for restaurant={}: {}", restaurantId, e.getMessage());
            return false;
        }
    }

    public List<SubscriptionResponse> getAllActiveSubscriptions() {
        return subscriptionRepository.findByCurrentSubscriptionTrue()
                .stream()
                .map(sub -> mapToRestaurantSubscriptionResponse(sub, sub.getPlans()))
                .toList();
    }

    private String ensureRazorpayCustomer(Long restaurantId, RestaurantInformation restaurant)
            throws RazorpayException {
        if (restaurant.getRazorpayCustomerId() != null
                && !restaurant.getRazorpayCustomerId().isBlank()) {
            return restaurant.getRazorpayCustomerId();
        }

        JSONObject custData = new JSONObject()
                .put("name", restaurant.getName())
                .put("email",   restaurant.getEmail())
                .put("contact", restaurant.getPhone())
                .put("notes",   new JSONObject().put("restaurant_id", restaurantId));

        Customer customer   = razorpayClient.customers.create(custData);
        String   customerId = customer.get("id");
        log.info("Created Razorpay customer {} for restaurant={}", customerId, restaurantId);

        restaurantClient.updateRazorpayCustomer(restaurantId, customerId);
        return customerId;
    }

    private void extractAndSetCounts(Subscription sub, com.razorpay.Subscription rzpSub) {
        setIfPresent(rzpSub.get("paid_count"),      n -> sub.setPaidCount(n.intValue()));
        setIfPresent(rzpSub.get("remaining_count"), n -> sub.setRemainingCount(n.intValue()));
        setIfPresent(rzpSub.get("charge_at"),       n -> sub.setChargeAt(n.longValue()));
        setIfPresent(rzpSub.get("current_end"),     n -> sub.setCurrentEnd(n.longValue()));
        setIfPresent(rzpSub.get("current_start"),   n -> sub.setCurrentStart(n.longValue()));
    }

    private void setIfPresent(Object val, java.util.function.Consumer<Number> setter) {
        if (val instanceof Number n) setter.accept(n);
    }

    private void sendStatusChangeNotification(Long restaurantId,
                                              SubscriptionStatus oldStatus,
                                              SubscriptionStatus newStatus) {
        try {
            log.debug("Status change notification: restaurant={} {} → {}", restaurantId, oldStatus, newStatus);
        } catch (Exception e) {
            log.warn("Notification failed for restaurant={}: {}", restaurantId, e.getMessage());
        }
    }

    private RestaurantInformation getRestaurant(Long restaurantId) {
        return restaurantClient.getRestaurantInformation(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant not found by id : " + restaurantId));
    }

    private Subscription getCurrentSubscription(Long restaurantId) {
        return subscriptionRepository
                .findByRestaurantIdAndCurrentSubscriptionTrue(restaurantId)
                .orElseThrow(() -> new SubscriptionException(
                        "No active subscription found for restaurant: " + restaurantId,
                        "SUBSCRIPTION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private SubscriptionStatus mapRazorpayStatus(String status) {
        return switch (status.toLowerCase()) {
            case "active"        -> SubscriptionStatus.ACTIVE;
            case "authenticated" -> SubscriptionStatus.AUTHENTICATED;
            case "cancelled"     -> SubscriptionStatus.CANCELLED;
            case "completed"     -> SubscriptionStatus.COMPLETED;
            case "expired"       -> SubscriptionStatus.EXPIRED;
            case "halted"        -> SubscriptionStatus.HALTED;
            case "pending"       -> SubscriptionStatus.PENDING;
            default              -> SubscriptionStatus.CREATED;
        };
    }

    private String formatDate(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private SubscriptionResponse toSubscriptionResponse(com.zapiee.subscriptionservice.model.Subscription sub,
                                                        Plan plan,
                                                        String restaurantName) {
        SubscriptionResponse resp = SubscriptionResponse.builder()
                .id(sub.getId())
                .razorpaySubscriptionId(sub.getRazorpaySubscriptionId())
                .razorpayKeyId("rzp_test_TcnRgd5AX8G2Eu")
                .plan(planService.toPlanResponse(plan))
                .status(sub.getStatus())
                .shortUrl(sub.getShortUrl())
                .totalCount(sub.getTotalCount())
                .paidCount(sub.getPaidCount())
                .remainingCount(sub.getRemainingCount())
                .chargeAt(sub.getChargeAt())
                .currentEnd(sub.getCurrentEnd())
                .currentStart(sub.getCurrentStart())
                .isCurrentSubscription(sub.isCurrentSubscription())
                .createdAt(sub.getCreatedAt())
                .build();

        if (sub.getCurrentEnd() != null) {
            resp.setUpgradeStartsAtFormatted(formatDate(sub.getCurrentEnd()));
        }
        return resp;
    }

    private SubscriptionResponse mapToRestaurantSubscriptionResponse(
            Subscription sub, Plan plan) {

        return SubscriptionResponse.builder()
                .id(sub.getId())
                .razorpaySubscriptionId(sub.getRazorpaySubscriptionId())
                .plan(planService.toPlanResponse(plan))
                .status(sub.getStatus())
                .totalCount(sub.getTotalCount())
                .paidCount(sub.getPaidCount())
                .remainingCount(sub.getRemainingCount())
                .chargeAt(sub.getChargeAt())
                .currentEnd(sub.getCurrentEnd())
                .startAt(sub.getStartAt())
                .endAt(sub.getEndAt())
                .notes(sub.getNotes())
                .isCurrentSubscription(sub.isCurrentSubscription())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    public SubscriptionResponse createSubscriptionFallback(Long restaurantId,
                                                           CreateSubscriptionRequest req,
                                                           Exception ex) {
        log.error("Circuit breaker open for subscription creation | restaurant={}: {}",
                restaurantId, ex.getMessage());
        throw new SubscriptionException(
                "Payment service temporarily unavailable. Please try again shortly.",
                "SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public SubscriptionResponse startFreeTrialFallback(Long restaurantId, String planId, Exception ex) {
        log.error("Circuit breaker open for trial start | restaurant={}: {}", restaurantId, ex.getMessage());
        throw new SubscriptionException(
                "Unable to start your trial right now. Please try again shortly.",
                "SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
