package com.zapiee.paymentservice.dto.request;

import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentRequest {
    @NotBlank(message = "order id required")
    private Long
            orderId;
    private String paymentRecordId;

    @NotBlank(message = "Table number is required")
    private String tableNumber;

    private OrderPaymentStatus orderPaymentStatus;
}
