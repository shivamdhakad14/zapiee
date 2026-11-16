package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PublicMenuResponse {
    private Long restaurantId;
    private String restaurantName;
    private Long tableId;
    private Long tableNumber;
    private String sessionToken;
    private LocalDateTime sessionExpiresAt;
    private List<PublicCategoryResponse> categories;
}
