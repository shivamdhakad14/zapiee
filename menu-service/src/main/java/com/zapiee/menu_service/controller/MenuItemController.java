package com.zapiee.menu_service.controller;

import com.zapiee.menu_service.dto.request.CreateMenuItemRequest;
import com.zapiee.menu_service.dto.request.MenuItemFilterRequest;
import com.zapiee.menu_service.dto.request.UpdateMenuItemRequest;
import com.zapiee.menu_service.dto.response.*;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.service.MenuItemService;
import com.zapiee.menu_service.util.MenuSortResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
                                                                        @Valid @RequestBody CreateMenuItemRequest request){
        log.info("request received for creating menu item by restaurant : {}", restaurantId);
        MenuItemResponse response = menuItemService.createMenuItem(request, restaurantId);
        return ResponseEntity.ok(ApiResponse.created(response, "item create successfully"));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
                                                                        @PathVariable @NotNull Long itemId,
                                                                        @Valid @RequestBody UpdateMenuItemRequest request){
        log.info("request received for update menu item by restaurant : {}", restaurantId);
        MenuItemResponse response = menuItemService.updateMenuItem(itemId, request, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response, "item update successfully"));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> deleteMenuItem(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
                                                                        @PathVariable @NotNull Long itemId){
        log.info("request received for delete menu item by restaurant : {}", restaurantId);
        menuItemService.deleteMenuItem(itemId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(null, "item delete successfully"));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
                                                                        @PathVariable @NotNull Long itemId){
        log.info("request received for get menu item by restaurant : {}", restaurantId);
        MenuItemResponse response = menuItemService.getItemById(itemId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/categories/{categoryId}/items")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemResponse>>> getCategoryItems(
            @RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        PagedResponse<MenuItemResponse> menuItemResponses = menuItemService.
                getMenuItemByCategoryAndRestaurantId(restaurantId, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(menuItemResponses));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemResponse>>> listItems(
            @RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId,
            @ModelAttribute MenuItemFilterRequest filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = PageRequest.of(page, size, MenuSortResolver.resolveItemSort(sortBy, direction));
        PagedResponse<MenuItemResponse> itemsPaged = menuItemService.getItemsPaged(restaurantId, filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(itemsPaged));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MenuSummaryResponse>> getSummary(@RequestHeader(name = "X-RESTAURANT-ID") Long restaurantId) {
        MenuSummaryResponse summary = menuItemService.getSummary(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
