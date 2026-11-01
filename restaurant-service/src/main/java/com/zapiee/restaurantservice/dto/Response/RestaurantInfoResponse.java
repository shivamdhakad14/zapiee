package com.zapiee.restaurantservice.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Restaurant Response",
        description = "Restaurant information returned to the client.")
public class RestaurantInfoResponse {

    private Long restaurantId;
    private Double latitude;
    private Double longitude;

}
