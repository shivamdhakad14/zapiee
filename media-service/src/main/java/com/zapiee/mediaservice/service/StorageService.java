package com.zapiee.mediaservice.service;

import com.zapiee.mediaservice.enums.MediaOwnerType;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    UploadResult upload(MultipartFile file, Long tenantId, MediaOwnerType ownerType, Long ownerId);

    void delete(String publicId);

    record UploadResult(
            String publicId,
            String url,
            String secureUrl,
            String format,
            String resourceType,
            long bytes,
            Integer width,
            Integer height
    ) {
    }
}