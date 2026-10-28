package com.zapiee.menu_service.controller;


import com.zapiee.menu_service.dto.response.ApiResponse;
import com.zapiee.menu_service.dto.response.ImageSearchResponse;
import com.zapiee.menu_service.service.AggregatedImageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final AggregatedImageService imageService;

    public ImageController(AggregatedImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ImageSearchResponse>> search(
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "1") @Min(1) @Max(50) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int perPage,
            @RequestParam(defaultValue = "true") boolean food) {
        ImageSearchResponse search = imageService.search(query, page, perPage, food);
        return ResponseEntity.ok(ApiResponse.success(search));
    }
}