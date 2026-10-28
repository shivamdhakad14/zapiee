package com.zapiee.menu_service.dto.request;

import com.zapiee.menu_service.enums.ImageSource;

public record ImageResult(
        String id,
        ImageSource source,
        String url,
        String thumbnailUrl,
        String description,
        String photographer,
        String photographerUrl,
        String pageUrl) {
}