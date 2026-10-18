package com.zapiee.orderservice.exception;

public class SessionClosedException extends BadRequestException {

    public SessionClosedException(String message) {
        super(message, ErrorCode.SESSION_CLOSED);
    }
}

