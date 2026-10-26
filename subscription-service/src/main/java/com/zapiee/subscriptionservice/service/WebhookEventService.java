package com.zapiee.subscriptionservice.service;

import com.zapiee.subscriptionservice.dto.response.WebhookEventResponse;
import com.zapiee.subscriptionservice.model.WebhookEvent;
import com.zapiee.subscriptionservice.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    public List<WebhookEventResponse> getAllWebhookEvent(){
        List<WebhookEvent> events = webhookEventRepository.
                findByProcessedFalseOrderByReceivedAtAsc();
        return events.stream().map(event -> {
           return WebhookEventResponse.builder()
                    .id(event.getId())
                    .razorpayEventId(event.getRazorpayEventId())
                    .eventType(event.getEventType())
                    .payload(event.getPayload())
                    .receivedAt(event.getReceivedAt())
                    .retryCount(event.getRetryCount())
                    .processed(event.isProcessed())
                    .processedAt(event.getProcessedAt())
                    .processingError(event.getProcessingError())
                    .createdAt(event.getCreatedAt())
                    .updatedAt(event.getUpdatedAt()).build();
        }).collect(Collectors.toList());
    }
}
