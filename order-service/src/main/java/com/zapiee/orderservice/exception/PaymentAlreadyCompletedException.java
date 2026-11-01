package com.zapiee.orderservice.exception;

public class PaymentAlreadyCompletedException extends ConflictException {

    public PaymentAlreadyCompletedException(String message) {
        super(
                message,
                ErrorCode.PAYMENT_ALREADY_COMPLETED
        );
    }
}

