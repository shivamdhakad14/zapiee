package com.zapiee.tableservice.dto.response;
import com.zapiee.tableservice.enums.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "TableTokenResponse",
        description = "Table information resolved from a QR token."
)
public class TableTokenResponse {

    private Long restaurantId;

    private Long tableId;

    private Long tableNumber;

    private Boolean active;

    private TableStatus status;
}