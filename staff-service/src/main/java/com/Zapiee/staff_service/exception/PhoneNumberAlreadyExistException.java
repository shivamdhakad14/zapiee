package com.Zapiee.staff_service.exception;

public class PhoneNumberAlreadyExistException extends RuntimeException {
    public PhoneNumberAlreadyExistException(String message) {
        super(message);
    }
}
