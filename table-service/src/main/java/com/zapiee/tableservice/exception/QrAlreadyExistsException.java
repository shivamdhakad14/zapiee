package com.zapiee.tableservice.exception;

public class QrAlreadyExistsException extends RuntimeException {
    public QrAlreadyExistsException(String message) {
        super(message);
    }
}