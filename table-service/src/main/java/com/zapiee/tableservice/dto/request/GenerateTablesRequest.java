package com.zapiee.tableservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "GenerateTablesRequest",
        description = "Request used to generate multiple restaurant tables along with their QR codes."
)
public class GenerateTablesRequest {

    @NotNull(message = "Number of tables is required")
    @Positive(message = "Number of tables must be greater than zero")
    private Long numberOfTables;

}