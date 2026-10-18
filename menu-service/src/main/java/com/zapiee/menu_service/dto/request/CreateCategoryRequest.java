package com.zapiee.menu_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Pattern(
            regexp = "^(https?://).+\\.(jpg|jpeg|png|webp)$",
            message = "Image URL must be a valid http(s) link ending in jpg, jpeg, png, or webp"
    )
    private String imageUrl;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

}
