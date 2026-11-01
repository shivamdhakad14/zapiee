package com.zapiee.menu_service.controller;

import com.zapiee.menu_service.dto.request.CreateCategoryRequest;
import com.zapiee.menu_service.dto.response.ApiResponse;
import com.zapiee.menu_service.dto.response.CategoryResponse;
import com.zapiee.menu_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
            @Valid @RequestBody CreateCategoryRequest request){
        log.info("request received for creating category by restaurant : {}", restaurantId);
        CategoryResponse category = categoryService.createCategory(request, restaurantId);
        return ResponseEntity.ok(ApiResponse.created(category, "category create successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId){
        log.info("request arrived for categories by restaurant : {}", restaurantId);
        List<CategoryResponse> response = categoryService.getCategoryByRestaurantId(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
                                                                         @PathVariable Long categoryId){
        log.info("request arrived for category by restaurant and catId: {}", restaurantId);
        CategoryResponse category = categoryService.getCategoryByCategory(categoryId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

}
