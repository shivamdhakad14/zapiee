package com.zapiee.menu_service.dto.response;


import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class ExtractedMenuItem {
    private String category;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean isVeg;
    private Double confidence;
}