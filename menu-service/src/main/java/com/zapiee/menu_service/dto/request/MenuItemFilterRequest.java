package com.zapiee.menu_service.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuItemFilterRequest {
    private Long categoryId;
    private Boolean isAvailable;
    private Boolean isVeg;
    private String spiceLevel;
    private String search;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private Boolean outOfStock;
}