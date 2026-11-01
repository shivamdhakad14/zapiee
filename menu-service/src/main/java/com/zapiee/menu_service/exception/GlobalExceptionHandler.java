package com.zapiee.menu_service.exception;


import com.zapiee.menu_service.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.BAD_REQUEST, ex.getErrorCode())
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OutsideGeofenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleOutsideGeofence(OutsideGeofenceException ex) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.FORBIDDEN, "FORBIDDEN")
                .withTraceId(traceId())
                .withPath("/api/v1/tables/resolve");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    @ExceptionHandler(InvalidQrTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidQrTokenException ex) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.NOT_FOUND, "NOT_FOUND")
                .withTraceId(traceId())
                .withPath("/api/v1/tables/resolve");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    @ExceptionHandler(LowLocationAccuracyException.class)
    public ResponseEntity<ApiResponse<Void>> handleLowAccuracy(LowLocationAccuracyException ex) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.BAD_REQUEST, "LOW_LOCATION_ACCURACY")
                .withTraceId(traceId())
                .withPath("/api/v1/tables/resolve");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(TableServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(TableServiceUnavailableException ex) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE")
                .withTraceId(traceId())
                .withPath("/api/v1/tables/resolve");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    @ExceptionHandler(ImageSearchUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(ImageSearchUnavailableException ex) {
        log.error("Image search unavailable: {}", ex.getMessage());

        ApiResponse<Void> body = ApiResponse.<Void>error("Image search is temporarily unavailable. Please try again shortly.",
                        HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE")
                .withTraceId(traceId())
                .withPath("/api/v1/tables/resolve");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    private String traceId() {
        String mdcTraceId = org.slf4j.MDC.get("traceId");
        return mdcTraceId != null ? mdcTraceId : UUID.randomUUID().toString();
    }
}
