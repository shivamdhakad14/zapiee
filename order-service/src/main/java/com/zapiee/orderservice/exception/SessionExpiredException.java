package com.zapiee.orderservice.exception;

public class SessionExpiredException extends BadRequestException {

    public SessionExpiredException(String message) {
        super(message, ErrorCode.SESSION_EXPIRED);
    }
}

