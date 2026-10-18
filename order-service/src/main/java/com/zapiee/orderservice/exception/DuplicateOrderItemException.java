package com.zapiee.orderservice.exception;

public class DuplicateOrderItemException extends ConflictException {

    public DuplicateOrderItemException(String message) {
        super(
                message,
                ErrorCode.DUPLICATE_ORDER_ITEM
        );
    }
}

