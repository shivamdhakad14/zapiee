package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PublicMenuResponse {
    private Long restaurantId;
    private Long tableId;
    private String sessionToken;
    private LocalDateTime sessionExpiresAt;
    private List<PublicCategoryResponse> categories;
}
