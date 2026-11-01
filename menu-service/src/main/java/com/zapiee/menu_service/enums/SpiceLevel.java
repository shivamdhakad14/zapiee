package com.zapiee.menu_service.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SpiceLevel {
    MILD, MEDIUM, HOT, EXTRA_HOT;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
