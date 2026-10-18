package com.zapiee.menu_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_import_staged_items", indexes = {
        @Index(name = "idx_staged_job", columnList = "import_job_id, review_status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuImportStagedItem {

    public enum ReviewStatus { PENDING, APPROVED, REJECTED, EDITED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "source_page_number")
    private Integer sourcePageNumber;

    @Column(name = "raw_category_name", length = 100)
    private String rawCategoryName;

    @Column(name = "raw_item_name", nullable = false, length = 150)
    private String rawItemName;

    @Column(name = "raw_description", length = 1000)
    private String rawDescription;

    @Column(name = "raw_price", precision = 10, scale = 2)
    private BigDecimal rawPrice;

    @Column(name = "raw_is_veg")
    private Boolean rawIsVeg;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "final_category_name", length = 100)
    private String finalCategoryName;

    @Column(name = "final_item_name", length = 150)
    private String finalItemName;

    @Column(name = "final_description", length = 1000)
    private String finalDescription;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "final_is_veg")
    private Boolean finalIsVeg;

    @Column(name = "possible_duplicate_of_item_id")
    private Long possibleDuplicateOfItemId;

    @Column(name = "cross_page_duplicate_of_staged_item_id")
    private Long crossPageDuplicateOfStagedItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;
}
