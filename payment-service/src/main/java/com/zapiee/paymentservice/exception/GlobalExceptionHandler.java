package com.zapiee.paymentservice.exception;


import com.zapiee.paymentservice.dto.response.ApiResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

        @ExceptionHandler(FeignException.class)
        public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex, HttpServletRequest request) {

            int statusCode = ex.status();
            HttpStatus status = HttpStatus.resolve(statusCode);

            if (status == null || statusCode < 400) {
                status = HttpStatus.BAD_GATEWAY;
            }

            String responseBody = ex.contentUTF8();

            HttpHeaders headers = new HttpHeaders();

            ApiResponse<Void> body = ApiResponse.<Void>error(responseBody, status, status.toString())
                    .withTraceId(traceId())
                    .withPath(request.getRequestURI());

            return ResponseEntity.status(status).body(body);
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

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex, HttpServletRequest request) {

        ApiResponse<Void> body = ApiResponse.<Void>error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR")
                .withTraceId(traceId())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(ex.getHttpStatus()).body(body);
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

    private String traceId() {
        String mdcTraceId = org.slf4j.MDC.get("traceId");
        return mdcTraceId != null ? mdcTraceId : UUID.randomUUID().toString();
    }
}
