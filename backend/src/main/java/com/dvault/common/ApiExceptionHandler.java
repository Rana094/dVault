package com.dvault.common;

import com.dvault.auth.DuplicateAccountException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(
            error.getField().equals("passwordWithinLimit") ? "password" : error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "VALIDATION_FAILED", "Check the supplied fields",
            request.getRequestURI(), Instant.now(), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "INVALID_REQUEST", "Request body is invalid", request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateAccountException.class)
    ResponseEntity<ApiError> duplicate(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, "EMAIL_CONFLICT", "This email is already registered", request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(401, "INVALID_CREDENTIALS", "Invalid email or password", request.getRequestURI()));
    }
}
