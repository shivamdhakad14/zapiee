package com.zapiee.orderservice.exception;

public class ResourceNotFoundException extends OrderServiceException {

    public ResourceNotFoundException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }
}

