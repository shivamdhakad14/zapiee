package com.zapiee.menu_service.exception;

public class BillingServiceUnavailableException extends RuntimeException {
    public BillingServiceUnavailableException(String message) {
        super(message);
    }
}
