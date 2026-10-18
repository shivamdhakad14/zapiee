package com.zapiee.orderservice.exception;

public class OrderModificationNotAllowedException extends ConflictException {

    public OrderModificationNotAllowedException(String message) {
        super(
                message,
                ErrorCode.ORDER_MODIFICATION_NOT_ALLOWED
        );
    }
}

