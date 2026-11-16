package com.zapiee.paymentservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentInitResponse {

    private String     paymentRecordId;     // Internal DB id
    private Long     orderId;             // Your internal order id
    private BigDecimal amount;
    private String     currency;

    private String restaurantName;
    private String restaurantLogoUrl;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String description;

}
