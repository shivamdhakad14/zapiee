package com.zapiee.menu_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_import_jobs", indexes = {
        @Index(name = "idx_import_job_restaurant", columnList = "restaurant_id, status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuImportJob {

    public enum Status { QUEUED, PROCESSING, READY_FOR_REVIEW, PARTIALLY_APPROVED, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;

    @Column(name = "pages_completed", nullable = false)
    @Builder.Default
    private Integer pagesCompleted = 0;

    @Column(name = "pages_failed", nullable = false)
    @Builder.Default
    private Integer pagesFailed = 0;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "total_items_extracted")
    private Integer totalItemsExtracted;

    @Column(name = "items_approved")
    private Integer itemsApproved;

    @Column(name = "requested_by_id", nullable = false)
    private Long requestedById;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Version
    private Integer version;
}