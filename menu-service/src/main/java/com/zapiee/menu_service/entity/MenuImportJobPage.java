package com.zapiee.menu_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_import_job_pages", indexes = {
        @Index(name = "idx_import_page_job", columnList = "import_job_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuImportJobPage {

    public enum PageStatus { QUEUED, PROCESSING, PROCESSED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_url", nullable = false, length = 1500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PageStatus status = PageStatus.QUEUED;

    @Column(name = "items_extracted")
    private Integer itemsExtracted;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
