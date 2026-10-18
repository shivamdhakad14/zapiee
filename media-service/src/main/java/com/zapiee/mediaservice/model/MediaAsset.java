package com.zapiee.mediaservice.model;

import com.zapiee.mediaservice.enums.MediaAssetStatus;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.enums.StorageProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    @Builder.Default
    private StorageProvider provider = StorageProvider.CLOUDINARY;

    @Column(name = "public_id", nullable = false, length = 512)
    private String publicId;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "secure_url", nullable = false, columnDefinition = "text")
    private String secureUrl;

    @Column(name = "format", length = 16)
    private String format;

    @Column(name = "resource_type", nullable = false, length = 16)
    @Builder.Default
    private String resourceType = "image";

    @Column(name = "bytes")
    private Long bytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private MediaAssetStatus status = MediaAssetStatus.ACTIVE;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
