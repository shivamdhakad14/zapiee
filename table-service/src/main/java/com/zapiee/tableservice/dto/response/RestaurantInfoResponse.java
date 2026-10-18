package com.zapiee.tableservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private BigDecimal latitude;
    private BigDecimal longitude;


}
