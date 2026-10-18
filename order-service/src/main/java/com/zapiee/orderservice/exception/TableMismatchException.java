package com.zapiee.orderservice.exception;

public class TableMismatchException extends ConflictException {

    public TableMismatchException(String message) {
        super(
                message,
                ErrorCode.TABLE_MISMATCH
        );
    }
}

