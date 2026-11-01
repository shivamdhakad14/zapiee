package com.zapiee.mediaservice.service.impl;

import com.zapiee.mediaservice.client.RestaurantClient;
import com.zapiee.mediaservice.dto.MediaAssetDto;
import com.zapiee.mediaservice.dto.MediaProperties;
import com.zapiee.mediaservice.dto.MenuImagesUploadResponse;
import com.zapiee.mediaservice.dto.RestaurantInformation;
import com.zapiee.mediaservice.enums.MediaAssetStatus;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.exception.ForbiddenOperationException;
import com.zapiee.mediaservice.exception.MediaNotFoundException;
import com.zapiee.mediaservice.exception.ResourceNotFoundException;
import com.zapiee.mediaservice.model.MediaAsset;
import com.zapiee.mediaservice.model.MediaIdempotencyKey;
import com.zapiee.mediaservice.repository.MediaAssetRepository;
import com.zapiee.mediaservice.repository.MediaIdempotencyKeyRepository;
import com.zapiee.mediaservice.service.MediaService;
import com.zapiee.mediaservice.service.StorageService;
import com.zapiee.mediaservice.validation.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class MediaServiceImpl implements MediaService{

    private static final List<String> WRITE_ROLES = List.of("PLATFORM_ADMIN", "RESTAURANT_OWNER", "STAFF_MANAGER");

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaIdempotencyKeyRepository idempotencyKeyRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final MediaProperties mediaProperties;
    private final RestaurantClient restaurantClient;

    @Override
    public MediaAsset upload(Long restaurantId, Long userId, MediaOwnerType ownerType, Long ownerId, MultipartFile file, String idempotencyKey) {

//        RestaurantInformation restaurant = getRestaurant(restaurantId);
        fileValidator.validate(file);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent() && existing.get().getRestaurantId().equals(restaurantId)) {
                log.info("Idempotent replay for key={}, returning existing media asset", idempotencyKey);
                return mediaAssetRepository.findActiveByIdAndRestaurant(existing.get().getMediaAssetId(), restaurantId)
                        .orElseThrow(() -> new MediaNotFoundException(existing.get().getMediaAssetId()));
            }
        }

        StorageService.UploadResult uploaded = storageService.upload(file, restaurantId, ownerType, ownerId);

        MediaAsset asset = MediaAsset.builder()
                .restaurantId(restaurantId)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .publicId(uploaded.publicId())
                .url(uploaded.url())
                .secureUrl(uploaded.secureUrl())
                .format(uploaded.format())
                .resourceType(uploaded.resourceType())
                .bytes(uploaded.bytes())
                .width(uploaded.width())
                .height(uploaded.height())
                .status(MediaAssetStatus.ACTIVE)
                .uploadedBy(userId)
                .build();

        mediaAssetRepository.save(asset);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeyRepository.save(MediaIdempotencyKey.builder()
                    .idempotencyKey(idempotencyKey)
                    .restaurantId(restaurantId)
                    .mediaAssetId(asset.getId())
                    .expiresAt(Instant.now().plus(mediaProperties.idempotencyTtlHours(), ChronoUnit.HOURS))
                    .build());
        }

        return asset;
    }

    @Override
    public MenuImagesUploadResponse uploadFullMenu(Long restaurantId, Long userId, MediaOwnerType ownerType,
                                                   MultipartFile file) {
//        RestaurantInformation restaurant = getRestaurant(restaurantId);
        fileValidator.validate(file);
        StorageService.UploadResult uploaded = storageService.upload(file, restaurantId, ownerType, userId);

        return MenuImagesUploadResponse.builder()
                .url(uploaded.url())
                .bytes(uploaded.bytes())
                .build();
    }

    @Override
    public MediaAsset confirmExternalUpload(Long restaurantId, Long userId, MediaOwnerType ownerType, Long ownerId, String publicId, String url,
                                            String secureUrl, String format, Long bytes, Integer width, Integer height) {
        RestaurantInformation restaurant = getRestaurant(restaurantId);
        String expectedPrefix = restaurant.getRestaurantId().toString();

        if (!publicId.contains(expectedPrefix)) {
            throw new ForbiddenOperationException("public_id does not belong to this tenant's upload folder");
        }

        MediaAsset asset = MediaAsset.builder()
                .restaurantId(restaurantId)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .publicId(publicId)
                .url(url)
                .secureUrl(secureUrl)
                .format(format)
                .bytes(bytes)
                .width(width)
                .height(height)
                .status(MediaAssetStatus.ACTIVE)
                .uploadedBy(userId)
                .build();

        mediaAssetRepository.save(asset);

        return asset;
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAsset getActive(Long mediaId, Long restaurantId) {
        return mediaAssetRepository.findActiveByIdAndRestaurant(mediaId, restaurantId)
                .orElseThrow(() -> new MediaNotFoundException(mediaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaAsset> listForOwner(MediaOwnerType ownerType, Long ownerId, Long restaurantId) {
        return mediaAssetRepository.findActiveByOwner(restaurantId, ownerType, ownerId);
    }

    @Override
    public void softDelete(Long mediaId, Long restaurantId) {

        MediaAsset asset = mediaAssetRepository.findActiveByIdAndRestaurant(mediaId, restaurantId)
                .orElseThrow(() -> new MediaNotFoundException(mediaId));

        asset.setStatus(MediaAssetStatus.DELETED);
        asset.setDeletedAt(Instant.now());
        mediaAssetRepository.save(asset);

        try {
            storageService.delete(asset.getPublicId());
        } catch (RuntimeException e) {
            log.warn("Soft-deleted media {} but Cloudinary cleanup failed; publicId={} needs reconciliation",
                    asset.getId(), asset.getPublicId(), e);
        }
    }

    private RestaurantInformation getRestaurant(Long restaurantId) {
        return restaurantClient.getRestaurantInformation(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant not found by id : " + restaurantId));
    }
}
