package com.zapiee.mediaservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.zapiee.mediaservice.dto.CloudinaryProperties;
import com.zapiee.mediaservice.dto.MediaProperties;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.exception.StorageProviderException;
import com.zapiee.mediaservice.service.StorageService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Transformation;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final MediaProperties mediaProperties;

    @Override
    public UploadResult upload(MultipartFile file, Long tenantId, MediaOwnerType ownerType, Long ownerId) {
        return CompletableFuture.supplyAsync(() -> doUpload(file, tenantId, ownerType, ownerId)).join();
    }

    private UploadResult doUpload(MultipartFile file, Long restaurantId, MediaOwnerType ownerType, Long ownerId) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", buildFolder(restaurantId, ownerType));
            options.put("public_id", buildPublicId(ownerId));
            options.put("overwrite", true);
            options.put("resource_type", "image");
            options.put("unique_filename", false);
            options.put("use_filename", false);

            applyTransformation(options, ownerType);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);

            return new UploadResult(
                    (String) result.get("public_id"),
                    (String) result.get("url"),
                    (String) result.get("secure_url"),
                    (String) result.get("format"),
                    (String) result.get("resource_type"),
                    ((Number) result.get("bytes")).longValue(),
                    result.get("width") != null ? ((Number) result.get("width")).intValue() : null,
                    result.get("height") != null ? ((Number) result.get("height")).intValue() : null
            );
        } catch (IOException e) {
            throw new StorageProviderException("Cloudinary upload failed", e);
        }
    }

    private String buildFolder(Long restaurantId, MediaOwnerType ownerType) {
        return "%s/%s/%s".formatted(cloudinaryProperties.folderPrefix(), restaurantId, ownerType.name().toLowerCase());
    }

    private String buildPublicId(Long ownerId) {
        return ownerId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void applyTransformation(Map<String, Object> options, MediaOwnerType ownerType) {

        Transformation transformation = switch (ownerType) {
            case MENU_ITEM, MENU_CATEGORY -> new Transformation()
                    .width(1200)
                    .height(1200)
                    .crop("limit")
                    .quality("auto")
                    .fetchFormat("auto");

            case RESTAURANT_LOGO -> new Transformation()
                    .width(800)
                    .height(800)
                    .crop("limit")
                    .quality("auto")
                    .fetchFormat("auto");

            case MENU_CATALOG  ->   new Transformation()
                    .width(1200)
                    .height(1600)
                    .crop("limit")
                    .quality("auto")
                    .fetchFormat("auto");

            case RECEIPT, DOCUMENT -> null;

        };

        if (transformation != null) {
            options.put("eager", List.of(transformation));
        }
    }

    @Override
    @CircuitBreaker(name = "cloudinary", fallbackMethod = "deleteFallback")
    @Retry(name = "cloudinary")
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            throw new StorageProviderException("Cloudinary delete failed for " + publicId, e);
        }
    }

    private UploadResult uploadFallback(MultipartFile file, UUID tenantId, MediaOwnerType ownerType, UUID ownerId, Throwable t) {
        log.error("Cloudinary upload circuit open/failed for tenant={} ownerType={} ownerId={}", tenantId, ownerType, ownerId, t);
        throw new StorageProviderException("Image storage is temporarily unavailable", t);
    }

    private void deleteFallback(String publicId, Throwable t) {
        log.error("Cloudinary delete circuit open/failed for publicId={}", publicId, t);
        throw new StorageProviderException("Image storage is temporarily unavailable", t);
    }
}
