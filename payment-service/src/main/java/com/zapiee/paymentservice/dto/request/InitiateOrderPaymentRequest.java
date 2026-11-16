package com.zapiee.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateOrderPaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Table number is required")
    private Long tableNumber;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    // Customer info (optional but helps with receipt)
    private String customerName;
    private String customerEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String customerPhone;

    @Size(max = 500)
    private String description;

}
