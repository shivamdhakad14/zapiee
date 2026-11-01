package com.zapiee.tableservice.dto.request;


import com.zapiee.tableservice.enums.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UpdateTableStatusRequest",
        description = "Request used to change the operational status of a restaurant table."
)
public class UpdateTableStatusRequest {

    @NotNull(message = "Table status is required")
    private TableStatus status;
}