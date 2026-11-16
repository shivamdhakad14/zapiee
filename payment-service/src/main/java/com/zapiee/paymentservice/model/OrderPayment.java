package com.zapiee.paymentservice.model;

import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import com.zapiee.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_payments",
        indexes = {
                @Index(name = "idx_op_order_id",     columnList = "orderId", unique = true),
                @Index(name = "idx_op_restaurant",   columnList = "restaurantId"),
                @Index(name = "idx_op_status",       columnList = "orderPaymentStatus")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Long restaurantId;

    // Order identifiers
    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long tableNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    // Customer info
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Payment states
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderPaymentStatus orderPaymentStatus = OrderPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String paymentMethod; // "upi" | "card" | "netBanking"
    private String failureReason;

    private String receipt;
    private String description;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

}
