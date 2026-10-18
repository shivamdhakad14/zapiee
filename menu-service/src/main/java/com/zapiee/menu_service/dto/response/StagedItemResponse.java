package com.zapiee.menu_service.dto.response;


import com.zapiee.menu_service.entity.MenuImportStagedItem;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class StagedItemResponse {
    private Long id;
    private Integer sourcePageNumber;
    private String categoryName;
    private String itemName;
    private String description;
    private BigDecimal price;
    private Boolean isVeg;
    private Double confidenceScore;
    private boolean lowConfidence;
    private Long possibleDuplicateOfItemId;
    private Long crossPageDuplicateOfStagedItemId;
    private MenuImportStagedItem.ReviewStatus reviewStatus;
}