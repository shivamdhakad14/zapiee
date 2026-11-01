package com.zapiee.orderservice.exception;

public class InvalidOrderStatusException extends BadRequestException {

    public InvalidOrderStatusException(String message) {
        super(message, ErrorCode.INVALID_ORDER_STATUS);
    }
}
