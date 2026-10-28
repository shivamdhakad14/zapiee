package com.zapiee.menu_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResolveResponse {
    private Long restaurantId;
    private Long tableId;
    private String sessionToken;
    private LocalDateTime expireAt;
}
