package com.zapiee.orderservice.exception;

public class BadRequestException extends OrderServiceException {

    public BadRequestException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }
}

