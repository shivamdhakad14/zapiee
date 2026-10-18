package com.zapiee.orderservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "order_item",
        indexes = {
                @Index(
                        name = "idx_order_item_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_item_item",
                        columnList = "item_id"
                ),
                @Index(
                        name = "idx_order_item_order_item",
                        columnList = "order_id, item_id"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent order.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_item_order"
            )
    )
    @NotNull
    private Order order;

    /**
     * Snapshot of menu item name at the time of ordering.
     */
    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    @NotBlank
    @Size(max = 255)
    private String name;

    /**
     * Price of one unit at the time of ordering.
     */
    @Column(
            name = "price",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;

    /**
     * Number of units ordered.
     */
    @Column(
            name = "quantity",
            nullable = false
    )
    @NotNull
    @Min(value = 1)
    @Max(value = 9999)
    private Integer quantity;

    /**
     * Snapshot of menu item image URL.
     */
    @Column(
            name = "image_url",
            length = 1000
    )
    @Size(max = 1000)
    private String imageUrl;

    /**
     * Currency used for this item.
     */
    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
}
