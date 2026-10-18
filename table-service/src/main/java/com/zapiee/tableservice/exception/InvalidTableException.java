package com.zapiee.tableservice.exception;

public class InvalidTableException extends RuntimeException {
    public InvalidTableException(String message) {
        super(message);
    }
}