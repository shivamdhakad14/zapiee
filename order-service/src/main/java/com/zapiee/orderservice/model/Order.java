
package com.zapiee.orderservice.model;

import com.zapiee.orderservice.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_order_uuid",
                        columnNames = "order_uuid"
                )
        },
        indexes = {
                @Index(name = "idx_restaurant_status",
                        columnList = "restaurant_id, status"),
                @Index(name = "idx_restaurant_table",
                        columnList = "restaurant_id, table_id"),
                @Index(name = "idx_session_id", columnList = "session_id"),
                @Index(name = "idx_restaurant_session",
                        columnList = "restaurant_id, session_id"),
                @Index(name = "idx_session_token", columnList = "session_token"),
                @Index(name = "idx_restaurant_created_at",
                        columnList = "restaurant_id, created_at")
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

    @NotBlank
    @Column(name = "order_uuid", nullable = false, length = 36, updatable = false)
    private String orderId;

    @NotNull
    @Positive
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @NotNull
    @Positive
    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @NotNull
    @Positive
    @Column(name = "table_number", nullable = false)
    private Long tableNumber;

    @NotNull
    @Positive
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @NotBlank
    @Column(name = "session_token", nullable = false)
    private String sessionToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @DecimalMin("0.00")
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;

    @DecimalMin("0.00")
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @DecimalMin("0.00")
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @DecimalMin("0.00")
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "is_payment", nullable = false)
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

    @OneToMany(
            mappedBy = "order",
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

    public void addItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }

        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        if (item != null) {
            items.remove(item);
            item.setOrder(null);
        }
    }
}
