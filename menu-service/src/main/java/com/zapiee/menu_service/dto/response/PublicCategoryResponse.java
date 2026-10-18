package com.zapiee.menu_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicCategoryResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private List<PublicMenuItemResponse> items;
    private int totalItemsInCategory;
}
