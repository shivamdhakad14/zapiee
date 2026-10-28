package com.zapiee.menu_service.service;

import com.zapiee.menu_service.client.TableClient;
import com.zapiee.menu_service.dto.request.TableResolveRequest;
import com.zapiee.menu_service.dto.response.PublicCategoryResponse;
import com.zapiee.menu_service.dto.response.PublicMenuItemResponse;
import com.zapiee.menu_service.dto.response.PublicMenuResponse;
import com.zapiee.menu_service.dto.response.TableResolveResponse;
import com.zapiee.menu_service.entity.Category;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.exception.BusinessRuleViolationException;
import com.zapiee.menu_service.mapper.MenuItemMapper;
import com.zapiee.menu_service.repository.CategoryRepository;
import com.zapiee.menu_service.repository.MenuItemRepository;
import com.zapiee.menu_service.validation.RestaurantValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicMenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantValidation restaurantValidation;
    private final MenuItemMapper menuItemMapper;
    private final TableClient tableClient;

    public PublicMenuResponse getPublicMenu(TableResolveRequest request){
        TableResolveResponse tableResolveResponse;
        log.info("scan qr request received for restaurant : {}", request.getQrToken());
        try{
             tableResolveResponse = tableClient.resolveTable(request);
             log.info("table resolve success : {}", tableResolveResponse.getRestaurantId());
        } catch (RuntimeException e) {
            log.error("table resolve failed");
            throw new BusinessRuleViolationException(e.getMessage(), HttpStatus.BAD_REQUEST.toString());
        }

        // restaurantValidation.validateRestaurantExist(tableResolveResponse.getRestaurantId());
        log.info("table resolve response : {}", tableResolveResponse.getRestaurantId());

        List<Category> categories = categoryRepository
                .findByRestaurantIdAndIsDeletedFalseAndActiveTrueOrderByDisplayOrderAsc(tableResolveResponse.getRestaurantId());

        List<PublicCategoryResponse> categoryResponses = categories.stream()
                .map(this::buildPublicCategory)
                .filter(c -> !c.getItems().isEmpty())
                .collect(Collectors.toList());

        return PublicMenuResponse.builder()
                .restaurantId(tableResolveResponse.getRestaurantId())
                .tableId(tableResolveResponse.getTableId())
                .sessionExpiresAt(tableResolveResponse.getExpireAt())
                .sessionToken(tableResolveResponse.getSessionToken())
                .categories(categoryResponses)
                .build();
    }

    private PublicCategoryResponse buildPublicCategory(Category category) {

        List<MenuItem> itemPage = menuItemRepository
                .findByCategoryIdAndIsDeletedFalseAndIsAvailableTrue(category.getId());

        return PublicCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .items(itemPage.stream().map(this::toPublicResponse).toList())
                .totalItemsInCategory((int) itemPage.size())
                .build();
    }

    public PublicMenuItemResponse toPublicResponse(MenuItem item) {
        if (item == null) return null;

        return PublicMenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .name(item.getName())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .price(item.getPrice())
                .taxRate(item.getTaxRate())
                .currency(item.getCurrency())
                .isVeg(item.getIsVeg())
                .spiceLevel(item.getSpiceLevel() != null ? item.getSpiceLevel().name() : null)
                .calories(item.getCalories())
                .prepTimeMinutes(item.getPrepTimeMinutes())
                .build();
    }
}
