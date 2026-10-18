package com.zapiee.mediaservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Backs the Idempotency-Key contract used across the platform (see Order/Payment services):
 * a retried "upload menu image" call with the same key returns the original result instead
 * of creating a duplicate Cloudinary asset.
 */
@Entity
@Table(name = "media_idempotency_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaIdempotencyKey {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "restaurantId", nullable = false)
    private Long restaurantId;

    @Column(name = "media_asset_id", nullable = false)
    private Long mediaAssetId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
