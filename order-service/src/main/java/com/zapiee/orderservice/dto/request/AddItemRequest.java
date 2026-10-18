package com.zapiee.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
        name = "Add Item Request",
        description = "Request used to add one or more menu items to an existing order."
)
public class AddItemRequest {

    @NotNull
    @Positive
    private Long id;

    @NotEmpty(message = "Items list must not be empty")
    @Size(max = 50, message = "You can add a maximum of 50 items in one request")
    @Valid
    private List<OrderItemDTO> items;

}

