package com.lofo.serenia.exception.exceptions;

import jakarta.ws.rs.core.Response;
import lombok.Getter;

/**
 * Unified exception for all Serenia errors.
 * Carries HTTP status code, error code, and user-friendly message in English.
 */
@Getter
public class SereniaException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;

    public SereniaException(String message, int httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public SereniaException(String message, int httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static SereniaException unauthorized(String message) {
        return new SereniaException(message, Response.Status.UNAUTHORIZED.getStatusCode(), "UNAUTHORIZED");
    }

    public static SereniaException forbidden(String message) {
        return new SereniaException(message, Response.Status.FORBIDDEN.getStatusCode(), "FORBIDDEN");
    }

    public static SereniaException badRequest(String message) {
        return new SereniaException(message, Response.Status.BAD_REQUEST.getStatusCode(), "BAD_REQUEST");
    }

    public static SereniaException internalError(String message, Throwable cause) {
        return new SereniaException(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "INTERNAL_ERROR", cause);
    }

    public static SereniaException notFound(String message) {
        return new SereniaException(message, Response.Status.NOT_FOUND.getStatusCode(), "NOT_FOUND");
    }

    public static SereniaException conflict(String message) {
        return new SereniaException(message, Response.Status.CONFLICT.getStatusCode(), "CONFLICT");
    }
}

