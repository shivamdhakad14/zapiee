package com.zapiee.menu_service.dto.response;

import java.util.List;
import java.util.Map;

public final class MenuExtractionSchema {

    public static Map<String, Object> build() {
        return Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "category", Map.of("type", "STRING"),
                                "name", Map.of("type", "STRING"),
                                "description", Map.of("type", "STRING", "nullable", true),
                                "price", Map.of("type", "NUMBER"),
                                "isVeg", Map.of("type", "BOOLEAN", "nullable", true),
                                "confidence", Map.of("type", "NUMBER")
                        ),
                        "required", List.of("category", "name", "price", "confidence")
                )
        );
    }

    private MenuExtractionSchema() {}
}
