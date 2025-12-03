package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.ws.rs.core.Response;

/**
 * Strategy interface for handling specific exception types.
 * Implementations should be application-scoped beans to enable auto-discovery.
 * Each exception type can have its own handler without modifying the global handler.
 */
public interface ExceptionHandler {

    /**
     * Determines if this handler can handle the given exception.
     *
     * @param exception the exception to check
     * @return true if this handler can process this exception
     */
    boolean canHandle(Throwable exception);

    /**
     * Processes the exception and returns an error response.
     *
     * @param exception the exception to handle
     * @param path      the request path that caused the exception
     * @param traceId   correlation ID for tracking
     * @return error response for the client
     */
    ErrorResponse handle(Throwable exception, String path, String traceId);

    /**
     * Returns the HTTP status code for this error.
     *
     * @return Response.Status code
     */
    Response.Status getStatus();

    /**
     * Priority of this handler.
     * Higher priority handlers are checked first.
     * Default: 0 (lowest priority)
     *
     * @return priority value
     */
    default int priority() {
        return 0;
    }
}

