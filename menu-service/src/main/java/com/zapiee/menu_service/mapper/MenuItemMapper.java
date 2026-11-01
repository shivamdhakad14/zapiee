package com.zapiee.menu_service.mapper;

import com.zapiee.menu_service.dto.response.MenuItemResponse;
import com.zapiee.menu_service.dto.response.PublicMenuItemResponse;
import com.zapiee.menu_service.dto.response.PublicMenuResponse;
import com.zapiee.menu_service.entity.MenuItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    MenuItemResponse toResponse(MenuItem menuItem);
    PublicMenuItemResponse toResponseForPublicMenu(MenuItem menuItem);

}
