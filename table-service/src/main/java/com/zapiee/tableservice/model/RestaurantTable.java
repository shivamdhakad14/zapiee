package com.zapiee.tableservice.model;


import com.zapiee.tableservice.enums.TableStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurant_tables",
        indexes = {
                @Index(name = "idx_restaurant_tables_restaurant_id", columnList = "restaurant_id"),
                @Index(name = "idx_restaurant_tables_active", columnList = "active"),
                @Index(name = "idx_restaurant_tables_status", columnList = "status")
        })
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private Long tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;


    @OneToOne(mappedBy = "restaurantTable", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private TableQr tableQr;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    @Column(updatable = false)
    private String createdBy;


    @Column(length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = TableStatus.AVAILABLE;
        }

        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = LocalDateTime.now();
    }
}