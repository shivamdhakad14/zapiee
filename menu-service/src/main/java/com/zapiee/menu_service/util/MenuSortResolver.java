package com.zapiee.menu_service.util;

import org.springframework.data.domain.Sort;

import java.util.Map;

public final class MenuSortResolver {

    private static final Map<String, String> ITEM_SORT_FIELDS = Map.of(
            "displayOrder", "displayOrder",
            "name", "name",
            "price", "price",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "stockCount", "stockCount"
    );

    private static final Map<String, String> CATEGORY_SORT_FIELDS = Map.of(
            "displayOrder", "displayOrder",
            "name", "name",
            "createdAt", "createdAt"
    );

    public static Sort resolveItemSort(String sortBy, String direction) {
        String field = ITEM_SORT_FIELDS.getOrDefault(sortBy, "displayOrder");
        return direction.equalsIgnoreCase("desc") ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    public static Sort resolveCategorySort(String sortBy, String direction) {
        String field = CATEGORY_SORT_FIELDS.getOrDefault(sortBy, "displayOrder");
        return direction.equalsIgnoreCase("desc") ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    private MenuSortResolver() {}
}
