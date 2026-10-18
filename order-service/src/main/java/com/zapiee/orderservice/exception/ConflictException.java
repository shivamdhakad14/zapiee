package com.zapiee.orderservice.exception;

public class ConflictException extends OrderServiceException {

    public ConflictException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }
}

