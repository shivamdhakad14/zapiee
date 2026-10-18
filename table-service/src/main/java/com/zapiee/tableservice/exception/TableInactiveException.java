package com.zapiee.tableservice.exception;

public class TableInactiveException extends RuntimeException {
    public TableInactiveException(String message) {
        super(message);
    }
}