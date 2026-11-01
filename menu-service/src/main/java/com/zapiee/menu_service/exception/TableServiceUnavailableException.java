package com.zapiee.menu_service.exception;

public class TableServiceUnavailableException extends RuntimeException {
    public TableServiceUnavailableException(String message) {
        super(message);
    }
}
