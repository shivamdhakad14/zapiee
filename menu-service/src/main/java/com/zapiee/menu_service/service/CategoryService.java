package com.zapiee.menu_service.service;

import com.zapiee.menu_service.dto.request.CreateCategoryRequest;
import com.zapiee.menu_service.dto.response.CategoryResponse;
import com.zapiee.menu_service.entity.Category;
import com.zapiee.menu_service.exception.DuplicateCategoryException;
import com.zapiee.menu_service.exception.ResourceNotFoundException;
import com.zapiee.menu_service.mapper.CategoryMapper;
import com.zapiee.menu_service.repository.CategoryRepository;
import com.zapiee.menu_service.validation.CategoryValidationService;
import com.zapiee.menu_service.validation.RestaurantValidation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryValidationService categoryValidationService;
    private final CategoryMapper categoryMapper;
    private final RestaurantValidation restaurantValidation;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, Long restaurantId) {
        // restaurantValidation.validateRestaurantExist(restaurantId);
        // categoryValidationService.validateForCreate(request, restaurantId);

        Category category = Category.builder()
                .restaurantId(restaurantId)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : null)
                .build();

        try {
            category = categoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCategoryException(
                    "A category named '" + request.getName() + "' already exists for this restaurant"
            );
        }

        return categoryMapper.toResponse(category);
    }

    public List<CategoryResponse> getCategoryByRestaurantId(Long restaurantId){
        // restaurantValidation.validateRestaurantExist(restaurantId);

        List<Category> categories = categoryRepository.
                findByRestaurantIdAndIsDeletedFalseAndActiveTrueOrderByDisplayOrderAsc(restaurantId);
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    public CategoryResponse getCategoryByCategory(Long categoryId, Long restaurantId){
        Category category = categoryRepository.findByIdAndRestaurantIdAndIsDeletedFalse(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("category not found"));
        return toResponseWithItemCount(category);
    }

    private CategoryResponse toResponseWithItemCount(Category c) {
        var counts = categoryRepository.getItemCounts(c.getId());
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .active(c.getActive())
                .displayOrder(c.getDisplayOrder())
                .itemCount(counts != null && counts.getTotal() != null ? counts.getTotal().intValue() : 0)
                .availableItemCount(counts != null && counts.getAvailable() != null ? counts.getAvailable().intValue() : 0)
                .build();
    }

}
