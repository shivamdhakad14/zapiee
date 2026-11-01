package com.zapiee.subscriptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class WebhookEventResponse {
    private String id;
    private String razorpayEventId;
    private String eventType;
    private String payload;
    private boolean processed;
    private String processingError;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private Integer retryCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}