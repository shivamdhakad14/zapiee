package com.zapiee.menu_service.dto.response;

import com.zapiee.menu_service.dto.request.ImageResult;
import com.zapiee.menu_service.enums.ImageSource;

import java.util.List;

public record ImageSearchResponse(
        String query,
        int page,
        List<ImageResult> results,
        List<ImageSource> failedSources,
        boolean partial) {
}