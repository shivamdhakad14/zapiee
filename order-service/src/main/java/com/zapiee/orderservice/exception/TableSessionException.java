package com.zapiee.orderservice.exception;

public class TableSessionException extends OrderServiceException {

    public TableSessionException(
            String message,
            ErrorCode errorCode
    ) {
        super(message, errorCode);
    }

    public TableSessionException(
            String message,
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(message, errorCode, cause);
    }
}
