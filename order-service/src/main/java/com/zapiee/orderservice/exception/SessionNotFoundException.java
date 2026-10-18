package com.zapiee.orderservice.exception;

public class SessionNotFoundException extends ResourceNotFoundException {

    public SessionNotFoundException(String message) {
        super(message, ErrorCode.SESSION_NOT_FOUND);
    }
}

