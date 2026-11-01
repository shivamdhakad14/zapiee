package com.zapiee.tableservice.exception;

public class QrNotFoundException extends RuntimeException {
    public QrNotFoundException(String message) {
        super(message);
    }
}