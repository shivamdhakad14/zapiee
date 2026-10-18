package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PublicMenuItemResponse {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal taxRate;
    private String currency;
    private Boolean isVeg;
    private String spiceLevel;
    private Integer calories;
    private Integer prepTimeMinutes;
}
