package com.zapiee.tableservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "zapiee.location")
public class LocationProperties {

    private BigDecimal allowedRadiusMeters = BigDecimal.valueOf(50.0);
    private Double maxGpsAccuracyMeters = 100.0;

}