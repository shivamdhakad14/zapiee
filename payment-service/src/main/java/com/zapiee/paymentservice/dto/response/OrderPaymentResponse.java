package com.zapiee.paymentservice.dto.response;

import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import com.zapiee.paymentservice.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentResponse {
    private String id;
    private Long restaurantId;
    private BigDecimal amount;
    private String currency;
    private String restaurantName;
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long orderId;
    private Long tableNumber;
    private OrderPaymentStatus orderPaymentStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
}