package com.zapiee.api_gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        log.error("Error occurred: Path={}, Message={}",
                request.path(),
                error.getMessage(),
                error);

        errorAttributes.put("timestamp", LocalDateTime.now());
        errorAttributes.put("path", request.path());
        errorAttributes.put("requestId", request.headers().firstHeader("X-Request-Id"));

        if (error instanceof org.springframework.web.server.ResponseStatusException) {
            HttpStatusCode status = ((ResponseStatusException) error).getStatusCode();
            errorAttributes.put("status", status.value());
            errorAttributes.put("error", status.toString());
        }

        // Remove stack trace from production responses
        errorAttributes.remove("trace");

        return errorAttributes;
    }
}
