package com.zapiee.mediaservice.config;

import com.cloudinary.Cloudinary;
import com.zapiee.mediaservice.dto.CloudinaryProperties;
import com.zapiee.mediaservice.dto.MediaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({CloudinaryProperties.class, MediaProperties.class})
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final CloudinaryProperties properties;

    @PostConstruct
    void validateCredentials() {
        if (isBlank(properties.cloudName()) || isBlank(properties.apiKey()) || isBlank(properties.apiSecret())) {
            throw new IllegalStateException(
                    "Cloudinary credentials are missing. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and " +
                            "CLOUDINARY_API_SECRET (via env vars / Kubernetes Secret / Vault) before starting media-service.");
        }
    }

    @Bean
    public Cloudinary cloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.cloudName());
        config.put("api_key", properties.apiKey());
        config.put("api_secret", properties.apiSecret());
        config.put("secure", properties.secure());
        return new Cloudinary(config);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
