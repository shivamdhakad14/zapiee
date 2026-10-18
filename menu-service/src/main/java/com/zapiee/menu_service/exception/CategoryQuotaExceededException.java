package com.zapiee.menu_service.exception;

public class CategoryQuotaExceededException extends RuntimeException {
    public CategoryQuotaExceededException(String message) {
        super(message);
    }
}
