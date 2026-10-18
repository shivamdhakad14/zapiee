 package com.zapiee.orderservice.exception;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public OrderServiceException(
            String message,
            ErrorCode errorCode
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderServiceException(
            String message,
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

