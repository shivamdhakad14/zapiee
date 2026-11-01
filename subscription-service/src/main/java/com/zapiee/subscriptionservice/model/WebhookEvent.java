package com.zapiee.subscriptionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events",
        indexes = {
                @Index(name = "idx_wh_rzp_event_id", columnList = "razorpayEventId", unique = true),
                @Index(name = "idx_wh_event_type", columnList = "eventType"),
                @Index(name = "idx_wh_processed", columnList = "processed")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String razorpayEventId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Builder.Default
    private boolean processed = false;

    private String processingError;

    @CreationTimestamp
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    private Integer retryCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;
    @UpdateTimestamp
    private Timestamp updatedAt;
}
