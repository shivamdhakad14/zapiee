package com.zapiee.orderservice.exception;

import com.zapiee.orderservice.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Business exceptions created by Order Service.
     */
    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderServiceException(
            OrderServiceException ex,
            HttpServletRequest request
    ) {

        HttpStatus status = resolveHttpStatus(ex.getErrorCode());

        log.warn(
                "Order service business exception. errorCode={}, message={}, path={}",
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        ex.getMessage(),
                        status,
                        ex.getErrorCode().getCode()
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .status(status)
                .body(response);
    }

    /**
     * @Valid request-body validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ApiResponse<Void> response = ApiResponse
                .<Void>validationError(
                        "Request validation failed.",
                        errors
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Constraint validation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations()
                .forEach(violation ->
                        errors.put(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        )
                );

        ApiResponse<Void> response = ApiResponse
                .<Void>validationError(
                        "Validation failed.",
                        errors
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Invalid JSON / malformed request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Malformed request body. path={}",
                request.getRequestURI()
        );

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        "Request body is invalid or malformed.",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.getCode()
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Invalid path variable or request parameter type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s'.",
                ex.getValue(),
                ex.getName()
        );

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        message,
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.getCode()
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Database/JPA errors.
     *
     * Do not expose SQL or database details to clients.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseException(
            DataAccessException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Database error. path={}",
                request.getRequestURI(),
                ex
        );

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        "A database error occurred. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.DATABASE_ERROR.getCode()
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Unknown/unexpected exception.
     *
     * Never expose internal exception details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        String traceId = generateTraceId();

        log.error(
                "Unexpected error. traceId={}, path={}",
                traceId,
                request.getRequestURI(),
                ex
        );

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        "An unexpected error occurred. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()
                )
                .withTraceId(traceId)
                .withPath(request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * No endpoint found.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {

        ApiResponse<Void> response = ApiResponse
                .<Void>error(
                        "Requested endpoint was not found.",
                        HttpStatus.NOT_FOUND,
                        ErrorCode.BAD_REQUEST.getCode()
                )
                .withTraceId(generateTraceId())
                .withPath(request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Maps business error codes to HTTP status codes.
     */
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {

        return switch (errorCode) {

            case ORDER_NOT_FOUND,
                 ORDER_ITEM_NOT_FOUND,
                 SESSION_NOT_FOUND,
                 RESTAURANT_NOT_FOUND,
                 TABLE_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;

            case RESTAURANT_ACCESS_DENIED,
                 FORBIDDEN,
                 WEBSOCKET_ACCESS_DENIED ->
                    HttpStatus.FORBIDDEN;

            case UNAUTHORIZED,
                 WEBSOCKET_SESSION_INVALID ->
                    HttpStatus.UNAUTHORIZED;

            case ORDER_ALREADY_EXISTS,
                 ORDER_MODIFICATION_NOT_ALLOWED,
                 INVALID_ORDER_STATUS,
                 DUPLICATE_ORDER_ITEM,
                 TABLE_MISMATCH,
                 PAYMENT_ALREADY_COMPLETED,
                 SESSION_MISMATCH ->
                    HttpStatus.CONFLICT;

            case SESSION_EXPIRED,
                 SESSION_CLOSED,
                 INVALID_SESSION,
                 ORDER_EMPTY,
                 INVALID_ORDER_ITEM,
                 PAYMENT_REQUIRED,
                 PAYMENT_NOT_COMPLETED,
                 INVALID_ROLE,
                 INVALID_REQUEST,
                 BAD_REQUEST ->
                    HttpStatus.BAD_REQUEST;

            case TABLE_SERVICE_UNAVAILABLE,
                 MENU_SERVICE_UNAVAILABLE ->
                    HttpStatus.SERVICE_UNAVAILABLE;

            case TABLE_SERVICE_ERROR ->
                    HttpStatus.BAD_GATEWAY;

            case DATABASE_ERROR,
                 INTERNAL_SERVER_ERROR,
                 WEBSOCKET_MESSAGE_INVALID ->
                    HttpStatus.INTERNAL_SERVER_ERROR;

            case VALIDATION_ERROR ->
                    HttpStatus.BAD_REQUEST;
        };
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}

