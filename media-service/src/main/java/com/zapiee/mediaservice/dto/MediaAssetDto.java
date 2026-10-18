package com.zapiee.mediaservice.dto;

import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.model.MediaAsset;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaAssetDto {
    private Long id;
    private MediaOwnerType ownerType;
    private Long ownerId;
    private String url;
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
    private Instant createdAt;

    public static MediaAssetDto from(MediaAsset asset) {
        return new MediaAssetDto(
                asset.getId(),
                asset.getOwnerType(),
                asset.getOwnerId(),
                asset.getSecureUrl(),
                asset.getFormat(),
                asset.getBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getCreatedAt()
        );
    }
}
