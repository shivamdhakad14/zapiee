package com.zapiee.tableservice.dto.response;

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
        name = "TableWithQrResponse",
        description = "Restaurant table information together with its QR code."
)
public class TableWithQrResponse {

    private Long tableId;

    private Long restaurantId;

    private Long tableNumber;
    private Boolean active;

    private TableStatus tableStatus;

    private TableQrResponse qr;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}