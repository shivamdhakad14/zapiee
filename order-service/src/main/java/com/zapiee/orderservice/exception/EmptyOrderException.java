package com.zapiee.orderservice.exception;

public class EmptyOrderException extends BadRequestException {

    public EmptyOrderException(String message) {
        super(
                message,
                ErrorCode.ORDER_EMPTY
        );
    }
}

