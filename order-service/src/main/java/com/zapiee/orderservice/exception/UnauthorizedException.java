package com.zapiee.orderservice.exception;

public class UnauthorizedException extends OrderServiceException {

    public UnauthorizedException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }
}

