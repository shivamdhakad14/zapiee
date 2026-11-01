package com.zapiee.tableservice.dto.request;

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
        name = "UpdateTableActiveRequest",
        description = "Request used to enable or disable a restaurant table."
)
public class UpdateTableActiveRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}