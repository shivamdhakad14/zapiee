package com.zapiee.tableservice.dto.response;

import com.zapiee.tableservice.enums.QrStatus;
import com.zapiee.tableservice.enums.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "TableResponse",
        description = "Restaurant table information."
)
public class TableResponse {

    private Long tableId;

    private Long restaurantId;

    private Long tableNumber;

    private TableStatus tableStatus;

    private Boolean active;

    private TableQrResponse qr;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
