package com.zapiee.menu_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class UpdateStagedItemRequest {
    @Size(max = 100)
    private String categoryName;
    @Size(max = 150)
    private String itemName;
    @Size(max = 1000)
    private String description;
    private BigDecimal price;
    private Boolean isVeg;
}
