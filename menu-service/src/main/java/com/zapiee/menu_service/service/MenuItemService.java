package com.zapiee.menu_service.service;

import com.zapiee.menu_service.dto.request.CreateMenuItemRequest;
import com.zapiee.menu_service.dto.request.MenuItemFilterRequest;
import com.zapiee.menu_service.dto.request.UpdateMenuItemRequest;
import com.zapiee.menu_service.dto.response.MenuItemResponse;
import com.zapiee.menu_service.dto.response.MenuSummaryResponse;
import com.zapiee.menu_service.dto.response.PagedResponse;
import com.zapiee.menu_service.entity.Category;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.enums.SpiceLevel;
import com.zapiee.menu_service.exception.BusinessRuleViolationException;
import com.zapiee.menu_service.exception.ResourceNotFoundException;
import com.zapiee.menu_service.mapper.MenuItemMapper;
import com.zapiee.menu_service.repository.CategoryRepository;
import com.zapiee.menu_service.repository.MenuItemRepository;
import com.zapiee.menu_service.validation.MenuItemValidationService;
import com.zapiee.menu_service.validation.RestaurantValidation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemValidationService menuItemValidationService;
    private final MenuItemMapper menuItemMapper;
    private final RestaurantValidation restaurantValidation;
    private final CategoryRepository categoryRepository;

    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request, Long restaurantId) {
        // restaurantValidation.validateRestaurantExist(restaurantId);
        // Category category = menuItemValidationService.validateForCreate(request, restaurantId);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        MenuItem item = MenuItem.builder()
                .restaurantId(restaurantId)
                .category(category)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .taxRate(request.getTaxRate())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .isVeg(request.getIsVeg())
                .spiceLevel(request.getSpiceLevel() != null ? request.getSpiceLevel() : SpiceLevel.MEDIUM)
                .calories(request.getCalories())
                .prepTimeMinutes(request.getPrepTimeMinutes())
                .stockCount(request.getStockCount())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        try {
            item = menuItemRepository.save(item);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException(
                    "An item with this name already exists in this category", "INVALID_ITEM");
        }

        return menuItemMapper.toResponse(item);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, UpdateMenuItemRequest request, Long restaurantId) {
        restaurantValidation.validateRestaurantExist(restaurantId);
        MenuItem existing = menuItemValidationService.validateForUpdate(itemId, restaurantId, request);

        MenuItemResponse before = menuItemMapper.toResponse(existing);

        if (request.getCategoryId() != null) {
            existing.setCategory(categoryReference(request.getCategoryId()));
        }

        if (request.getName() != null) existing.setName(request.getName().trim());
        if (request.getDescription() != null) existing.setDescription(request.getDescription().trim());
        if (request.getImageUrl() != null) existing.setImageUrl(request.getImageUrl());
        if (request.getPrice() != null) existing.setPrice(request.getPrice());
        if (request.getTaxRate() != null) existing.setTaxRate(request.getTaxRate());
        if (request.getIsVeg() != null) existing.setIsVeg(request.getIsVeg());
        if (request.getSpiceLevel() != null) existing.setSpiceLevel(request.getSpiceLevel());
        if (request.getCalories() != null) existing.setCalories(request.getCalories());
        if (request.getPrepTimeMinutes() != null) existing.setPrepTimeMinutes(request.getPrepTimeMinutes());
        if (request.getStockCount() != null) {
            menuItemValidationService.validateStockUpdate(request.getStockCount());
            existing.setStockCount(request.getStockCount());
        }
        if (request.getDisplayOrder() != null) existing.setDisplayOrder(request.getDisplayOrder());

        MenuItem saved;
        try {
            saved = menuItemRepository.save(existing);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException(
                    "Another item with this name already exists in that category", "INVALID_ITEM");
        }

        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    public void deleteMenuItem(Long itemId, Long restaurantId) {
        // restaurantValidation.validateRestaurantExist(restaurantId);
        MenuItem existing = menuItemValidationService.validateForDelete(itemId, restaurantId);

//        boolean isOnActiveOrder = orderServiceClient.isItemOnActiveOrder(itemId, restaurantId);
//        if (isOnActiveOrder) {
//            throw new BusinessRuleViolationException(
//                    "Cannot delete an item that is part of an active order", "ITEM_IN_USE");
//        }

        existing.softDelete();
        menuItemRepository.save(existing);
    }

    public PagedResponse<MenuItemResponse> getMenuItemByCategoryAndRestaurantId(Long restaurantId, Long categoryId, Pageable pageable){
        // restaurantValidation.validateRestaurantExist(restaurantId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("category not found"));
        Page<MenuItem> menuItems = menuItemRepository.
                findByRestaurantIdAndCategoryIdAndIsDeletedFalse(restaurantId, category.getId(), pageable);
        return mapToDto(menuItems);
    }

    public MenuItemResponse getItemById(Long itemId, Long restaurantId) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantIdAndIsDeletedFalse(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        return menuItemMapper.toResponse(item);
    }

    public PagedResponse<MenuItemResponse> getItemsPaged(Long restaurantId, MenuItemFilterRequest filter, Pageable pageable) {
        Page<MenuItem> items = menuItemRepository.findAll(
                MenuItemSpecificationsService.withFilters(restaurantId, filter), pageable);
        return mapToDto(items);
    }

    public MenuSummaryResponse getSummary(Long restaurantId) {
        long totalCategories = categoryRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        long activeCategories = categoryRepository.countByRestaurantIdAndIsDeletedFalseAndActiveTrue(restaurantId);
        long totalItems = menuItemRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        long availableItems = menuItemRepository.countByRestaurantIdAndIsDeletedFalseAndIsAvailableTrue(restaurantId);
        long unavailableItems = menuItemRepository.countByRestaurantIdAndIsDeletedFalseAndIsAvailableFalse(restaurantId);
        long outOfStock = menuItemRepository.countByRestaurantIdAndIsDeletedFalseAndStockCount(restaurantId, 0);

        return MenuSummaryResponse.builder()
                .totalCategories(totalCategories)
                .activeCategories(activeCategories)
                .totalItems(totalItems)
                .availableItems(availableItems)
                .unavailableItems(unavailableItems)
                .outOfStockItems(outOfStock)
                .build();
    }

    private PagedResponse<MenuItemResponse> mapToDto(Page<MenuItem> menuItems) {

        Page<MenuItemResponse> map = menuItems.map(menuItem -> MenuItemResponse.builder()
                .id(menuItem.getId())
                .currency(menuItem.getCurrency())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .displayOrder(menuItem.getDisplayOrder())
                .spiceLevel(menuItem.getSpiceLevel())
                .imageUrl(menuItem.getImageUrl())
                .isVeg(menuItem.getIsVeg())
                .stockCount(menuItem.getStockCount())
                .createdAt(menuItem.getCreatedAt())
                .isDeleted(menuItem.getIsDeleted())
                .prepTimeMinutes(menuItem.getPrepTimeMinutes())
                .calories(menuItem.getCalories())
                .categoryId(menuItem.getCategory().getId())
                .taxReate(menuItem.getTaxRate())
                .name(menuItem.getName())
                .isAvailable(menuItem.getIsAvailable())
                .updatedAt(menuItem.getUpdatedAt())
                .build()
        );
        return PagedResponse.from(map);
    }

    private Category categoryReference(Long categoryId) {
        Category ref = new Category();
        ref.setId(categoryId);
        return ref;
    }
}
