package com.zapiee.subscriptionservice.exception;

import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        ApiResponse<Void> body = ApiResponse.<Void>validationError("Validation failed", errors)
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(
            Exception ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error("Malformed request", HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>notFound(ex.getMessage())
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubscriptionException(SubscriptionException ex) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), ex.getHttpStatus(), ex.getErrorCode())
                .withTraceId(traceId())
                .withPath("/");

        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest request) {

        String trace = traceId();
        ApiResponse<Void> body = ApiResponse.<Void>error(
                        "An unexpected error occurred. Please try again later." + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR")
                .withTraceId(trace)
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String traceId() {
        String mdcTraceId = org.slf4j.MDC.get("traceId");
        return mdcTraceId != null ? mdcTraceId : UUID.randomUUID().toString();
    }
}
