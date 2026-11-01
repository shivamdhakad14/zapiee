package com.zapiee.menu_service.client;

import com.zapiee.menu_service.dto.response.MenuImagesUploadResponse;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service", url = "http://localhost:8080")
public interface FileUploadClient {

    @PostMapping(value = "/api/v1/media/bulk-menu/images", consumes = "multipart/form-data")
    MenuImagesUploadResponse uploadFullMenuImage(@RequestParam Long userId, @RequestParam Long restaurantId, @RequestPart("file") MultipartFile file);

    @GetMapping("/internal/v1/files/download")
    byte[] download(@RequestParam("url") String fileUrl);
}
