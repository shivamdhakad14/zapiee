package com.zapiee.orderservice.exception;

public class ForbiddenException extends OrderServiceException {

    public ForbiddenException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }
}

