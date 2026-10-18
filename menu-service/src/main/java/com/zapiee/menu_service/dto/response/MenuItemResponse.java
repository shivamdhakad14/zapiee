package com.zapiee.menu_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zapiee.menu_service.enums.SpiceLevel;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuItemResponse {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal taxReate;
    private String currency;
    private Boolean isVeg;
    private Boolean isAvailable;
    private Boolean isDeleted;
    private SpiceLevel spiceLevel;
    private Integer calories;
    private Integer prepTimeMinutes;
    private Integer stockCount;
    private Integer displayOrder;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
