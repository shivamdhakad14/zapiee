package com.zapiee.orderservice.exception;

public class InvalidOrderItemException extends BadRequestException {

    public InvalidOrderItemException(String message) {
        super(
                message,
                ErrorCode.INVALID_ORDER_ITEM
        );
    }
}

