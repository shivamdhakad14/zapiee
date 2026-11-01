package com.zapiee.menu_service.provider;

import com.zapiee.menu_service.dto.request.ImageResult;
import com.zapiee.menu_service.enums.ImageSource;

import java.util.List;

public interface ImageProvider {
    ImageSource source();

    List<ImageResult> search(String query, int page, int perPage);
}
