package com.zapiee.mediaservice.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        boolean secure,
        String folderPrefix,
        long uploadTimeoutMs
) {
}
