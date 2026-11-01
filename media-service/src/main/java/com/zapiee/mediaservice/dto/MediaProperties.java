package com.zapiee.mediaservice.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        List<String> allowedContentTypes,
        long maxFileSizeBytes,
        int maxImageDimension,
        Transformation menuImage,
        Transformation logo,
        long idempotencyTtlHours
) {
    public record Transformation(int transformationWidth, int transformationHeight, String crop,
                                  String quality, String fetchFormat) {
    }
}
