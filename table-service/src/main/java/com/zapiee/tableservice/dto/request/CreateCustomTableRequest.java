package com.zapiee.tableservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CreateCustomTableRequest",
        description = "Request used to create a single table with a custom table number."
)
public class CreateCustomTableRequest {

    @NotBlank(message = "Table number is required")
    private String tableNumber;

}