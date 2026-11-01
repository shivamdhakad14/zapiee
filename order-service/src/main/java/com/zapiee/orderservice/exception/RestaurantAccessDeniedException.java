package com.zapiee.orderservice.exception;

public class RestaurantAccessDeniedException extends ForbiddenException {

    public RestaurantAccessDeniedException(String message) {
        super(
                message,
                ErrorCode.RESTAURANT_ACCESS_DENIED
        );
    }
}

