package com.zapiee.menu_service.mapper;

import com.zapiee.menu_service.dto.response.CategoryResponse;
import com.zapiee.menu_service.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
