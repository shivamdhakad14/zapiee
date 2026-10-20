package com.zapiee.restaurantservice.client;

import com.zapiee.restaurantservice.dto.ApiResponse;
import com.zapiee.restaurantservice.dto.Response.MediaUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service")
public interface MediaClient {

    @PostMapping(value = "/api/v1/media/document/{documentId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<String> uploadIdentityDocument(@RequestPart("file") MultipartFile file,
                                               @PathVariable("documentId") Long documentId,
                                               @RequestParam("userId") Long userId,
                                               @RequestParam("restaurantId") Long restaurantId);


}