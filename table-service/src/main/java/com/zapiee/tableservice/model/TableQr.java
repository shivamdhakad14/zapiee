package com.zapiee.tableservice.model;

import com.zapiee.tableservice.enums.QrStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "table_qr_codes",

        indexes = {
                @Index(name = "idx_table_qr_token", columnList = "qr_token"),
                @Index(name = "idx_table_qr_table_id", columnList = "table_id"),
                @Index(name = "idx_table_qr_status", columnList = "status")
        })
public class TableQr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "table_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_table_qr_table"))
    private RestaurantTable restaurantTable;

    @Column(nullable = false, unique = true)
    private String qrToken;


    @Column(nullable = false)
    private String qrPayload;

    @Column(columnDefinition = "TEXT")
    private String qrImageBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QrStatus status = QrStatus.ACTIVE;


    @Column(nullable = false)
    private LocalDateTime generatedAt;

    private LocalDateTime revokedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private String createdBy;

    private String updatedBy;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.generatedAt == null) {
            this.generatedAt = now;
        }

        if (this.status == null) {
            this.status = QrStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = LocalDateTime.now();
    }
}