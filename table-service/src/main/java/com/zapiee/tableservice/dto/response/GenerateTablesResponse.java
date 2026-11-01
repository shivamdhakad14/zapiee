package com.zapiee.tableservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "GenerateTablesResponse",
        description = "Result of bulk table generation."
)
public class GenerateTablesResponse {

    private List<TableWithQrResponse> tables;

}
