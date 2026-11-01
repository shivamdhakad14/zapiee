package com.zapiee.menu_service.validation;

import com.zapiee.menu_service.client.SubscriptionClientWrapper;
import com.zapiee.menu_service.dto.request.CreateMenuItemRequest;
import com.zapiee.menu_service.dto.request.UpdateMenuItemRequest;
import com.zapiee.menu_service.dto.response.PlanFeaturesResponse;
import com.zapiee.menu_service.entity.Category;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.exception.BusinessRuleViolationException;
import com.zapiee.menu_service.exception.ResourceNotFoundException;
import com.zapiee.menu_service.repository.CategoryRepository;
import com.zapiee.menu_service.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuItemValidationService {

    private static final int UNLIMITED = -1;
    private final SubscriptionClientWrapper subscriptionClientWrapper;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    public Category validateForCreate(CreateMenuItemRequest request, Long restaurantId) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getRestaurantId().equals(restaurantId)) {
            throw new BusinessRuleViolationException("Category does not belong to this restaurant", "INVALID_CATEGORY");
        }
        if (Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessRuleViolationException("Cannot add items to a deleted category", "INVALID_OPERATION");
        }

        boolean nameExists = menuItemRepository
                .existsByCategoryIdAndNameIgnoreCaseAndIsDeletedFalse(
                        request.getCategoryId(), request.getName().trim());
        if (nameExists) {
            throw new BusinessRuleViolationException(
                    "An item named '" + request.getName() + "' already exists in this category", "INVALID_ITEM"
            );
        }

        long countInCategory = menuItemRepository
                .countByCategoryIdAndIsDeletedFalse(request.getCategoryId());

        PlanFeaturesResponse features = subscriptionClientWrapper.getFeatures(restaurantId);
        checkQuota(restaurantId, features.getMaxMenuItemsPerCategory(), countInCategory, "Menu item in category",
                "LIMIT_REACHED");

        long totalCount = menuItemRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        checkQuota(restaurantId, features.getMaxMenuItemsTotal(), totalCount, "Menu Item", "LIMIT_REACHED");

        return category;
    }

    public MenuItem validateForUpdate(Long itemId, Long restaurantId, UpdateMenuItemRequest request) {

        MenuItem existing = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!existing.ownedBy(restaurantId)) {
            throw new BusinessRuleViolationException("Menu item does not belong to this restaurant", "ACCESS_DENIED");
        }
        if (Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new ResourceNotFoundException("Menu item has been deleted");
        }

        Long targetCategoryId = request.getCategoryId() != null
                ? request.getCategoryId()
                : existing.getCategory().getId();

        if (request.getCategoryId() != null && !request.getCategoryId().equals(existing.getCategory().getId())) {
            Category targetCategory = categoryRepository.findById(targetCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target category not found"));
            if (!targetCategory.getRestaurantId().equals(restaurantId)) {
                throw new BusinessRuleViolationException("Target category does not belong to this restaurant", "INVALID_DATA");
            }
            if (Boolean.TRUE.equals(targetCategory.getIsDeleted())) {
                throw new BusinessRuleViolationException("Cannot move item into a deleted category", "INVALID_DATA");
            }
        }

        if (request.getName() != null) {
            boolean nameConflict = menuItemRepository
                    .existsByCategoryIdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(
                            targetCategoryId, request.getName().trim(), itemId);
            if (nameConflict) {
                throw new BusinessRuleViolationException("Another item with this name already exists in that category", "INVALID_DATA");
            }
        }

        return existing;
    }


    public MenuItem validateForDelete(Long itemId, Long restaurantId) {
        MenuItem existing = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!existing.ownedBy(restaurantId)) {
            throw new BusinessRuleViolationException("Menu item does not belong to this restaurant", "ACCESS_DENIED");
        }

        return existing;
    }


    public MenuItem validateForAvailabilityToggle(Long itemId, Long restaurantId) {
        MenuItem existing = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!existing.ownedBy(restaurantId)) {
            throw new BusinessRuleViolationException("Menu item does not belong to this restaurant", "ACCESS_DENIED");
        }
        if (Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessRuleViolationException("Cannot toggle availability of a deleted item", "INVALID_DATA");
        }
        return existing;
    }

    public void validateStockUpdate(Integer newStockCount) {
        if (newStockCount != null && newStockCount < 0) {
            throw new BusinessRuleViolationException("Stock count cannot be negative", "INVALID_DATA");
        }
    }

    private void checkQuota(Long restaurantId, long limit, long currentCount, String quotaName, String status) {

        if (limit == UNLIMITED) {
            return;
        }
        if (currentCount >= limit) {
            throw new BusinessRuleViolationException(
                    quotaName + " limit reached (" + limit + "). Upgrade your plan to add more.", status
            );
        }
    }

}
