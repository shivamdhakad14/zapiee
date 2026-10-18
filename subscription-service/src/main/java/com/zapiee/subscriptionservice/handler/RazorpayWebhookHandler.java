package com.zapiee.subscriptionservice.handler;

import com.zapiee.subscriptionservice.model.WebhookEvent;
import com.zapiee.subscriptionservice.repository.WebhookEventRepository;
import com.zapiee.subscriptionservice.service.SubscriptionService;
import com.zapiee.subscriptionservice.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookHandler {

    private final WebhookEventRepository webhookEventRepository;
    private final SubscriptionService subscriptionService;
    private final SignatureUtil signatureUtil;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean handleWebhook(String payload, String signature, String eventId) {
        // 1. Verify signature
        if (!signatureUtil.verifyWebhookSignature(payload, signature)) {
            log.error("INVALID WEBHOOK SIGNATURE — potential spoofing attempt");
            return false;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("event").asText();

            // 2. Idempotency check
            if (webhookEventRepository.existsByRazorpayEventId(eventId)) {
                log.info("Duplicate webhook event, skipping: {} ({})", eventId, eventType);
                return true;
            }

            // 3. Persist raw event first
            WebhookEvent event = WebhookEvent.builder()
                    .razorpayEventId(eventId)
                    .eventType(eventType)
                    .payload(payload)
                    .processed(false)
                    .retryCount(0)
                    .build();
            webhookEventRepository.save(event);

            // 4. Process asynchronously
            processEventAsync(event.getId(), eventType, root);
            return true;

        } catch (Exception e) {
            log.error("Webhook parse error: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void processEventAsync(String eventDbId, String eventType, JsonNode root) {
        WebhookEvent event = webhookEventRepository.findById(eventDbId).orElse(null);
        if (event == null) return;

        try {
            switch (eventType) {
                case "subscription.activated"   -> onSubscriptionEvent(root, "activated");
                case "subscription.charged"     -> onSubscriptionEvent(root, "charged");
                case "subscription.cancelled"   -> onSubscriptionEvent(root, "cancelled");
                case "subscription.halted"      -> onSubscriptionEvent(root, "halted");
                case "subscription.completed"   -> onSubscriptionEvent(root, "completed");
                case "subscription.paused"      -> onSubscriptionEvent(root, "paused");
                case "subscription.resumed"     -> onSubscriptionEvent(root, "resumed");
                case "subscription.pending"     -> onSubscriptionEvent(root, "pending");
                case "refund.processed"         -> onRefundProcessed(root);
                default -> log.info("Unhandled webhook event type: {}", eventType);
            }
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error processing webhook event {} ({}): {}", eventDbId, eventType, e.getMessage());
            event.setProcessingError(e.getMessage());
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
        }
        webhookEventRepository.save(event);
    }

    private void onSubscriptionEvent(JsonNode root, String event) {
        String subId = root.path("payload").path("subscription").path("entity").path("id").asText();
        log.info("Webhook: subscription.{} for subscription: {}", event, subId);
        subscriptionService.syncSubscriptionStatus(subId);
    }

    private void onRefundProcessed(JsonNode root) {
        String paymentId = root.path("payload").path("refund").path("entity").path("payment_id").asText();
        log.info("Webhook: Refund processed for payment: {}", paymentId);
    }
}
