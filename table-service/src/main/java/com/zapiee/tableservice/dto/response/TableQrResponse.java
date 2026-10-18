package com.zapiee.tableservice.dto.response;

import com.zapiee.tableservice.enums.QrStatus;
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
        name = "TableQrResponse",
        description = "QR code information associated with a restaurant table."
)
public class TableQrResponse {

    private Long qrId;

    private String qrToken;

    private String qrPayload;

    private String qrImageBase64;

    private QrStatus status;

    private LocalDateTime generatedAt;

    private LocalDateTime revokedAt;

}