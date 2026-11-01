package com.zapiee.menu_service.exception;

public class LowLocationAccuracyException extends RuntimeException {
    public LowLocationAccuracyException(String message) {
        super(message);
    }
}
