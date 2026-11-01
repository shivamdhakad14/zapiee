package com.Zapiee.staff_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PasswordMismatchException extends RuntimeException{
    public PasswordMismatchException(String message){
        super();
    }
}
