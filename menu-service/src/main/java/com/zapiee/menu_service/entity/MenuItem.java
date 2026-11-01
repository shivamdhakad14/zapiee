package com.zapiee.menu_service.entity;

import com.zapiee.menu_service.enums.SpiceLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(
        name = "menu_items",
        indexes = {
                @Index(name = "idx_menuitem_restaurant", columnList = "restaurant_id"),
                @Index(name = "idx_menuitem_restaurant_category", columnList = "restaurant_id, category_id"),
                @Index(name = "idx_menuitem_restaurant_available", columnList = "restaurant_id, is_available")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 1500)
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "is_veg", nullable = false)
    @Builder.Default
    private Boolean isVeg = true;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SpiceLevel spiceLevel = SpiceLevel.MEDIUM;

    private Integer calories;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "stock_count")
    private Integer stockCount;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public void softDelete() {
        this.isDeleted = true;
        this.isAvailable = false;
    }

    public void toggleAvailability() {
        this.isAvailable = !this.isAvailable;
    }

    public boolean ownedBy(Long restaurantId) {
        return this.restaurantId.equals(restaurantId);
    }
}