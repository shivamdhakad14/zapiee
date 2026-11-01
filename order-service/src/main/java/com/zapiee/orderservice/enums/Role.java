package com.zapiee.orderservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Role {

    STAFF,
    OWNER,
    CUSTOMER;

    @JsonCreator
    public static Role fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Role.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}