package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuSummaryResponse {
    private long totalCategories;
    private long activeCategories;
    private long totalItems;
    private long availableItems;
    private long unavailableItems;
    private long outOfStockItems;
}