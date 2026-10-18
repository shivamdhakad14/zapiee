package com.zapiee.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema( name = "WebSocket Message",
        description = "Generic WebSocket message exchanged between clients and the Order Service." )
public class WsMessage<T> {

    @NotNull(message = "Message type is required")
    private String type;

    @Valid
    private T payload;
}
