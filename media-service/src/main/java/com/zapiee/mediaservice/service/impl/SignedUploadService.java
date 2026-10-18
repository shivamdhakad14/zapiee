package com.zapiee.mediaservice.service.impl;

import com.cloudinary.Cloudinary;
import com.zapiee.mediaservice.dto.CloudinaryProperties;
import com.zapiee.mediaservice.dto.SignedUploadResponse;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignedUploadService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public SignedUploadResponse generateSignature(Long restaurantId, MediaOwnerType ownerType, UUID ownerId) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = "%s/%s/%s".formatted(cloudinaryProperties.folderPrefix(), restaurantId, ownerType.name().toLowerCase());
        String publicId = ownerId + "-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", folder);
        paramsToSign.put("public_id", publicId);
        paramsToSign.put("overwrite", true);

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        return  SignedUploadResponse.builder()
                .cloudName(cloudinary.config.cloudName)
                .apiKey(cloudinary.config.apiKey)
                .signature(signature)
                .timestamp(timestamp)
                .publicId(publicId)
                .folder(folder)
                .build();
    }
}
