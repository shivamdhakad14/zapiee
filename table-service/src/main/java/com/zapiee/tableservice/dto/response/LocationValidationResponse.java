package com.zapiee.tableservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "LocationValidationResponse",
        description = "Result of customer location validation."
)
public class LocationValidationResponse {

    private Boolean allowed;
    private BigDecimal distanceMeters;
    private BigDecimal allowedRadiusMeters;
    private String message;

}