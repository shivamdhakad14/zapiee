package com.zapiee.orderservice.dto.response;

import com.zapiee.orderservice.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema( name = "Table Session Response",
        description = "Table session information returned by Table Service and consumed by Order Service."
)
public class TableSessionResponse {

    private Long sessionId;
    private Long restaurantId;
    private Long tableId;
    private Long tableNumber;
    private String sessionToken;
    private SessionStatus sessionStatus;
    private LocalDateTime expiresAt;

}