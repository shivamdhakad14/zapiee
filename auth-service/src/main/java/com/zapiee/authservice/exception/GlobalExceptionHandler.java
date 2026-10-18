package com.zapiee.authservice.exception;

import com.zapiee.authservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
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

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>conflict(ex.getMessage())
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>unauthorized("Invalid email or password")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedAccount(
            LockedException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(
                        "Account is temporarily locked due to multiple failed login attempts",
                        HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledAccount(
            DisabledException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(
                        "Account is deactivated", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>forbidden("You do not have permission to perform this action")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest request) {

        String trace = traceId();
        log.error("Unhandled exception [traceId={}] at {}", trace, request.getRequestURI(), ex);

        ApiResponse<Void> body = ApiResponse.<Void>error(
                        "An unexpected error occurred. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR")
                .withTraceId(trace)
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleViolationException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode())
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    private String traceId() {
        String mdcTraceId = org.slf4j.MDC.get("traceId");
        return mdcTraceId != null ? mdcTraceId : UUID.randomUUID().toString();
    }
}
