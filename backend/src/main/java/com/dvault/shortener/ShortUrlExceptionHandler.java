package com.dvault.shortener;

import com.dvault.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ShortUrlController.class)
public class ShortUrlExceptionHandler {
    @ExceptionHandler(ShortUrlException.class)
    ResponseEntity<ApiError> handle(ShortUrlException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status).body(ApiError.of(exception.status.value(),
            exception.code, exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parameter(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "INVALID_PARAMETER",
            "URL identifiers and pagination parameters must be integers", request.getRequestURI()));
    }
}
