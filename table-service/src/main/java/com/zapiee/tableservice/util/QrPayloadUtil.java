package com.zapiee.tableservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QrPayloadUtil {

    @Value("${customer.ui.base-url}")
    private String customerUiBaseUrl;


    public String buildPayload(Long restaurantId, Long tableId, Long tableNumber, String qrToken) {

        if (restaurantId == null || restaurantId <= 0) {
            throw new IllegalArgumentException("Invalid restaurant ID.");
        }

        if (tableId == null || tableId <= 0) {
            throw new IllegalArgumentException("Invalid table ID.");
        }

        if (tableNumber == null || tableNumber <= 0) {
            throw new IllegalArgumentException("Invalid table number.");
        }

        if (qrToken == null || qrToken.isBlank()) {
            throw new IllegalArgumentException("QR token is required.");
        }

        if (customerUiBaseUrl == null || customerUiBaseUrl.isBlank()) {
            throw new IllegalStateException("Customer UI base URL is not configured.");
        }

        return customerUiBaseUrl
                + "?restaurantId=" + restaurantId
                + "&tableId=" + tableId
                + "&tableNumber=" + tableNumber
                + "&token=" + qrToken;
    }
}

