package com.zapiee.restaurantservice.dto.Response;


import com.zapiee.restaurantservice.enums.RestaurantStatus;
import com.zapiee.restaurantservice.enums.SubscriptionStatus;
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
public class RestaurantResponse {

    private Long id;

    private Long ownerId;

    @Schema(example = "Restaurant")
    private String businessType;

    @Schema(example = "OrderZap Family Restaurant")
    private String businessName;

    private String ownerName;

    private String businessContact;

    @Schema(example = "contact@orderzap.com")
    private String businessEmail;

    private String country;
    private String city;
    private String state;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer geofenceRadiusMeters;
    private String area;
    private String pincode;

    private String logoUrl;

    private RestaurantStatus restaurantStatus;
    private SubscriptionStatus subscriptionStatus;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
