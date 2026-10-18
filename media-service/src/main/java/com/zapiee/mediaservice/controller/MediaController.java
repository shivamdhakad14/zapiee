package com.zapiee.mediaservice.controller;

import com.zapiee.mediaservice.dto.ApiResponse;
import com.zapiee.mediaservice.dto.MediaAssetDto;
import com.zapiee.mediaservice.dto.MenuImagesUploadResponse;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.model.MediaAsset;
import com.zapiee.mediaservice.service.MediaService;
import com.zapiee.mediaservice.service.impl.SignedUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final SignedUploadService signedUploadService;

    @PostMapping(value = "/menu-items/{menuItemId}/images", consumes = "multipart/form-data")
    public ResponseEntity<MediaAssetDto> uploadMenuItemImage(
            @PathVariable Long menuItemId,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long restaurantId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        MediaAsset asset = mediaService.upload(restaurantId, userId, MediaOwnerType.MENU_ITEM, menuItemId, file, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaAssetDto.from(asset));
    }

    @PostMapping(value = "/document/{documentId}/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> uploadIdentityDocument(
            @PathVariable Long documentId,
            @RequestParam Long userId,
            @RequestParam Long restaurantId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        MediaAsset asset = mediaService.upload(restaurantId, userId, MediaOwnerType.DOCUMENT, documentId, file, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(asset.getUrl()));
    }

    @PostMapping(value = "/bulk-menu/images", consumes = "multipart/form-data")
    public ResponseEntity<MenuImagesUploadResponse> uploadFullMenuImage(
            @RequestParam Long userId,
            @RequestParam Long restaurantId,
            @RequestPart("file") MultipartFile file) {
        MenuImagesUploadResponse url = mediaService.uploadFullMenu(restaurantId, userId, MediaOwnerType.MENU_CATALOG, file);
        return ResponseEntity.status(HttpStatus.OK).body(url);
    }

}
