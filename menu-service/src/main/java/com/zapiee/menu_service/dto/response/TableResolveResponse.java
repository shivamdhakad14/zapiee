package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Builder
public class TableResolveResponse {
    private Long restaurantId;
    private Long tableId;
    private String sessionToken;
    private LocalDateTime expireAt;
}
