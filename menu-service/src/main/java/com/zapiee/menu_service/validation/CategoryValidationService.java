package com.zapiee.menu_service.validation;

import com.zapiee.menu_service.client.SubscriptionClientWrapper;
import com.zapiee.menu_service.dto.request.CreateCategoryRequest;
import com.zapiee.menu_service.dto.response.PlanFeaturesResponse;
import com.zapiee.menu_service.entity.Category;
import com.zapiee.menu_service.exception.*;
import com.zapiee.menu_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryValidationService {

    private static final int UNLIMITED = -1;

    private final CategoryRepository categoryRepository;
    private final SubscriptionClientWrapper billingClientWrapper;

    public void validateForCreate(CreateCategoryRequest request, Long restaurantId) {

        boolean nameExists = categoryRepository
                .existsByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(restaurantId, request.getName().trim());
        if (nameExists) {
            throw new DuplicateCategoryException(
                    "A category named '" + request.getName() + "' already exists for this restaurant"
            );
        }

        PlanFeaturesResponse features = billingClientWrapper.getFeatures(restaurantId);
        long activeCount = categoryRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);

        if (features.getMaxCategories() != UNLIMITED && activeCount >= features.getMaxCategories()) {
            throw new CategoryQuotaExceededException(
                    "Maximum number of categories (" + features.getMaxCategories() +
                            ") reached for your " + features.getPlanId() + " plan"
            );
        }
    }

    public void validateForUpdate(Long categoryId, Long restaurantId, CreateCategoryRequest request) {

        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!existing.getRestaurantId().equals(restaurantId)) {
            throw new BusinessRuleViolationException("Category does not belong to this restaurant", "INVALID_RESTAURANT");
        }

        boolean nameConflict = categoryRepository
                .existsByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(
                        restaurantId, request.getName().trim(), categoryId
                );

        if (nameConflict) {
            throw new DuplicateCategoryException(
                    "Another category with this name already exists"
            );
        }
    }

    public void validateForDelete(Long categoryId, Long restaurantId, long linkedMenuItemCount) {

        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!existing.getRestaurantId().equals(restaurantId)) {
            throw new BusinessRuleViolationException("Category does not belong to this restaurant", "INVALID_RESTAURANT");
        }

        if (linkedMenuItemCount > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot delete category with " + linkedMenuItemCount +
                            " active menu item(s). Move or delete items first.", "BAD_OPERATION"
            );
        }
    }

    public void validateBulkImportAllowed(Long restaurantId) {
        PlanFeaturesResponse features = billingClientWrapper.getFeatures(restaurantId);
        if (!features.isBulkMenuImportEnabled()) {
            throw new FeatureNotAvailableException("Bulk menu import requires PRO plan or higher");
        }
    }
}