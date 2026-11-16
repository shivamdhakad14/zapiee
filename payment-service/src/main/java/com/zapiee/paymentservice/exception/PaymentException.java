package com.zapiee.paymentservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public PaymentException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
