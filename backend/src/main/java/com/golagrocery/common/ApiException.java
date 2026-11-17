package com.golagrocery.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound(String what) { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found"); }
    public static ApiException badRequest(String msg) { return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg); }
    public static ApiException conflict(String msg) { return new ApiException(HttpStatus.CONFLICT, "CONFLICT", msg); }
    public static ApiException unauthorized(String msg) { return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", msg); }
    public static ApiException forbidden(String msg) { return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", msg); }
    public static ApiException tooMany(String msg) { return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", msg); }
}
