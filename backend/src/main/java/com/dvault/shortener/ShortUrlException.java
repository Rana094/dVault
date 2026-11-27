package com.dvault.shortener;

import org.springframework.http.HttpStatus;

public class ShortUrlException extends RuntimeException {
    final HttpStatus status;
    final String code;

    ShortUrlException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
