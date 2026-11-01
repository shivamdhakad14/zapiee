package com.zapiee.tableservice.dto.response;

import com.zapiee.tableservice.enums.SessionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSessionResponse {

    private Long sessionId;
    private Long restaurantId;
    private Long tableId;
    private Long tableNumber;
    private String sessionToken;
    private SessionStatus sessionStatus;
    private LocalDateTime expiresAt;
}