package com.zapiee.mediaservice.service;

import com.zapiee.mediaservice.dto.MediaAssetDto;
import com.zapiee.mediaservice.dto.MenuImagesUploadResponse;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.model.MediaAsset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MediaService {

    MediaAsset upload(Long restaurantId, Long userId, MediaOwnerType ownerType, Long ownerId,
                      MultipartFile file, String idempotencyKey);

    MenuImagesUploadResponse uploadFullMenu(Long restaurantId, Long userId, MediaOwnerType ownerType,
                                            MultipartFile file);

    MediaAsset confirmExternalUpload(Long restaurantId, Long userId, MediaOwnerType ownerType, Long ownerId,
                                      String publicId, String url, String secureUrl, String format,
                                      Long bytes, Integer width, Integer height);

    MediaAsset getActive(Long mediaId, Long restaurantId);

    List<MediaAsset> listForOwner(MediaOwnerType ownerType, Long ownerId, Long restaurantId);

    void softDelete(Long mediaId, Long restaurantId);
}
