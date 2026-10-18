package com.zapiee.menu_service.exception;

public class OutsideGeofenceException extends RuntimeException {
    public OutsideGeofenceException(String message) {
        super(message);
    }
}
