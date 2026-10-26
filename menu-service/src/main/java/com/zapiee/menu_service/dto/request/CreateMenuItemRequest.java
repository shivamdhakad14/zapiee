package com.zapiee.menu_service.dto.request;

import com.zapiee.menu_service.enums.SpiceLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuItemRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 150, message = "Item name must be between 2 and 150 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

//    @Pattern(
//            regexp = "^(https?://).+\\.(jpg|jpeg|png|webp)$",
//            message = "Image URL must be a valid http(s) link ending in jpg, jpeg, png, or webp"
//    )
    private String imageUrl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "99999.99", message = "Price exceeds maximum allowed value")
    @Digits(integer = 5, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax rate cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Tax rate must have at most 2 decimal places")
    private BigDecimal taxRate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code (e.g. INR)")
    private String currency;

    @NotNull(message = "isVeg must be specified")
    private Boolean isVeg;

    private SpiceLevel spiceLevel;

    @Min(value = 0, message = "Calories cannot be negative")
    @Max(value = 10000, message = "Calories value looks invalid")
    private Integer calories;

    @Min(value = 1, message = "Prep time must be at least 1 minute")
    @Max(value = 300, message = "Prep time seems unrealistic (max 300 minutes)")
    private Integer prepTimeMinutes;

    @Min(value = 0, message = "Stock count cannot be negative")
    private Integer stockCount;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;
}