package com.zapiee.menu_service.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "images")
public record ImageProperties(
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("7s") Duration aggregateTimeout,
        @Valid @NotNull ProviderSettings pexels,
        @Valid @NotNull ProviderSettings unsplash) {

    public record ProviderSettings(@NotBlank String baseUrl, @NotBlank String apiKey) {
        @Override
        public String toString() { // never leak keys into logs
            return "ProviderSettings[baseUrl=" + baseUrl + ", apiKey=****]";
        }
    }
}
