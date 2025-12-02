package com.lofo.serenia.exception.model;

import java.util.UUID;

/**
 * Unified error response model for all REST API errors.
 * Provides consistent error information to clients.
 */
public record ErrorResponse(
        String id,
        long timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId
) {
    /**
     * Creates an ErrorResponse with generated ID and current timestamp.
     */
    public static ErrorResponse of(int status, String error, String message, String path, String traceId) {
        return new ErrorResponse(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                status,
                error,
                message,
                path,
                traceId
        );
    }

    /**
     * Creates an ErrorResponse without trace ID (for non-sensitive errors).
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return of(status, error, message, path, null);
    }
}

