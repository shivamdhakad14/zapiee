package com.Zapiee.staff_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationErrors(
                MethodArgumentNotValidException ex) {

            Map<String, String> errors = new LinkedHashMap<>();

            ex.getBindingResult().getFieldErrors().forEach(error ->
                    errors.putIfAbsent(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            return ResponseEntity.badRequest().body(errors);
        }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "not found",
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<?> passwordMismatchException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "Password Mismatch",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> emailAlreadyExistsException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "email already exist",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<?> roleNotFoundException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "email already exist",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> invalidCredentialsException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "InvalidCredentialsException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<?> emailNotVerifiedException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "emailNotVerifiedException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<?> accountLockedException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "accountLockedException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<?> accountDisabledException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "accountDisabledException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<?> invalidTokenException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "invalidTokenException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PhoneNumberAlreadyExistException.class)
    public ResponseEntity<?> phoneNumberAlreadyExistException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "phoneNumberAlreadyExistException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> rateLimitExceededException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "RateLimitExceededException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<?> messagingException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "messagingException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<?> otpExpiredException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "otpExpiredException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<?> invalidOtpException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "invalidOtpException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> userNotFoundException(ResourceNotFoundException e , WebRequest request){
        ErrorResponse response = new ErrorResponse(
                new Date(),
                "userNotFoundException",
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

}
