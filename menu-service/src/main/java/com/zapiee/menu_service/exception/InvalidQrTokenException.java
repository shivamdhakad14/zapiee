package com.zapiee.menu_service.exception;

public class InvalidQrTokenException extends RuntimeException {
    public InvalidQrTokenException(String message) {
        super(message);
    }
}
