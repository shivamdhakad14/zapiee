package com.zapiee.tableservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResolveResponse {

    private Long restaurantId;
    private Long tableId;
    private String sessionToken;
    private LocalDateTime sessionExpiresAt;

}