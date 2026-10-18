package com.zapiee.menu_service.service;

import com.zapiee.menu_service.dto.request.MenuItemFilterRequest;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.enums.SpiceLevel;
import org.springframework.data.jpa.domain.Specification;

public class MenuItemSpecificationsService {

    public static Specification<MenuItem> forRestaurant(Long restaurantId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("restaurantId"), restaurantId),
                cb.isFalse(root.get("isDeleted"))
        );
    }

    public static Specification<MenuItem> withFilters(Long restaurantId, MenuItemFilterRequest f) {
        Specification<MenuItem> spec = forRestaurant(restaurantId);

        if (f.getCategoryId() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), f.getCategoryId()));
        }
        if (f.getIsAvailable() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isAvailable"), f.getIsAvailable()));
        }
        if (f.getIsVeg() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isVeg"), f.getIsVeg()));
        }
        if (f.getSpiceLevel() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("spiceLevel"), SpiceLevel.valueOf(f.getSpiceLevel())));
        }
        if (f.getMinPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), f.getMinPrice()));
        }
        if (f.getMaxPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), f.getMaxPrice()));
        }
        if (Boolean.TRUE.equals(f.getOutOfStock())) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("stockCount"), 0));
        }
        if (f.getSearch() != null && !f.getSearch().isBlank()) {
            String pattern = "%" + f.getSearch().trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }
        return spec;
    }
}
