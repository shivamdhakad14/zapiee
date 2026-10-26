package com.zapiee.tableservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QrPayloadUtil {

    @Value("${customer.ui.base-url}")
    private String customerUiBaseUrl;


    public String buildPayload(String qrToken) {

        if (qrToken == null || qrToken.isBlank()) {
            throw new IllegalArgumentException("QR token is required.");
        }

        if (customerUiBaseUrl == null || customerUiBaseUrl.isBlank()) {
            throw new IllegalStateException("Customer UI base URL is not configured.");
        }

        return customerUiBaseUrl
                + "&token=" + qrToken;
    }
}

