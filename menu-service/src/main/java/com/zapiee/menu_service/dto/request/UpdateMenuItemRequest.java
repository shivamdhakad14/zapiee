package com.zapiee.menu_service.dto.request;

import com.zapiee.menu_service.enums.SpiceLevel;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class UpdateMenuItemRequest {

    private Long categoryId;

    @Size(min = 2, max = 150, message = "Item name must be between 2 and 150 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Pattern(
            regexp = "^(https?://).+\\.(jpg|jpeg|png|webp)$",
            message = "Image URL must be a valid http(s) link ending in jpg, jpeg, png, or webp"
    )
    private String imageUrl;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "99999.99", message = "Price exceeds maximum allowed value")
    @Digits(integer = 5, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax rate cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Tax rate must have at most 2 decimal places")
    private BigDecimal taxRate;

    private Boolean isVeg;
    private SpiceLevel spiceLevel;

    @Min(value = 0, message = "Calories cannot be negative")
    private Integer calories;

    @Min(value = 1, message = "Prep time must be at least 1 minute")
    private Integer prepTimeMinutes;

    @Min(value = 0, message = "Stock count cannot be negative")
    private Integer stockCount;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;
}