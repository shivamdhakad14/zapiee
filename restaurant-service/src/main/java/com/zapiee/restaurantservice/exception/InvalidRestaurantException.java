package com.zapiee.restaurantservice.exception;

public class InvalidRestaurantException extends RuntimeException {

    public InvalidRestaurantException(String message) {
        super(message);
    }
}