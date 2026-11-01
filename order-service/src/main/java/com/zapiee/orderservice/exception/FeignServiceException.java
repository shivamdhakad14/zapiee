package com.zapiee.orderservice.exception;

public class FeignServiceException extends OrderServiceException {

    public FeignServiceException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }

    public FeignServiceException(
            String message,
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(message, errorCode, cause);
    }
}

