package com.zapiee.tableservice.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GeoLocationUtil {


    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public BigDecimal calculateDistanceMeters(BigDecimal customerLatitude, BigDecimal customerLongitude,
                                              BigDecimal restaurantLatitude, BigDecimal restaurantLongitude) {


        double lat1 = Math.toRadians(customerLatitude.doubleValue());
        double lat2 = Math.toRadians(restaurantLatitude.doubleValue());

        double deltaLatitude = Math.toRadians(restaurantLatitude
                        .subtract(customerLatitude)
                        .doubleValue());

        double deltaLongitude = Math.toRadians(restaurantLongitude
                        .subtract(customerLongitude)
                        .doubleValue());


        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                        + Math.cos(lat1) * Math.cos(lat2)
                                * Math.sin(deltaLongitude / 2)
                                * Math.sin(deltaLongitude / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_METERS * c;

        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }
}

// Haversine formula.
