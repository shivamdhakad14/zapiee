package com.zapiee.orderservice.model;

import com.zapiee.orderservice.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {

                @Index(
                        name = "idx_order_uuid",
                        columnList = "order_uuid"
                ),

                @Index(
                        name = "idx_restaurant_order",
                        columnList = "restaurant_id, order_uuid"
                ),

                @Index(
                        name = "idx_restaurant_status",
                        columnList = "restaurant_id, status"
                ),

                @Index(
                        name = "idx_restaurant_table",
                        columnList = "restaurant_id, table_id"
                ),

                @Index(
                        name = "idx_session_id",
                        columnList = "session_id"
                ),

                @Index(
                        name = "idx_restaurant_session",
                        columnList = "restaurant_id, session_id"
                ),

                @Index(
                        name = "idx_session_token",
                        columnList = "session_token"
                ),

                @Index(
                        name = "idx_restaurant_session_token",
                        columnList = "restaurant_id, session_token"
                ),

                @Index(
                        name = "idx_created_at",
                        columnList = "created_at"
                ),

                @Index(
                        name = "idx_restaurant_created_at",
                        columnList = "restaurant_id, created_at"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uuid", nullable = false)
    private String orderId;

    @Column(name = "restaurant_id", nullable = false)
    @Positive
    private Long restaurantId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "table_number", nullable = false)
    private Long tableNumber;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "session_token", nullable = false)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "is_payment", nullable = false)
    @Builder.Default
    private boolean payment = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();


    @PrePersist
    protected void generateOrderUUID() {

        if (orderId == null || orderId.isBlank()) {
            orderId = UUID.randomUUID().toString();
        }
    }
}