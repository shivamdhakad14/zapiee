package com.zapiee.tableservice.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GeoLocationUtil {


    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public double calculateDistanceMeters(Double customerLatitude, Double customerLongitude,
                                              Double restaurantLatitude, Double restaurantLongitude) {


        double dLat = Math.toRadians(restaurantLatitude - customerLatitude);
        double dLon = Math.toRadians(restaurantLongitude - customerLongitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(customerLatitude)) * Math.cos(Math.toRadians(restaurantLatitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}

// Haversine formula.
